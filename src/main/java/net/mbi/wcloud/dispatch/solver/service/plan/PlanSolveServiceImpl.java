package net.mbi.wcloud.dispatch.solver.service.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.*;
import net.mbi.wcloud.dispatch.solver.dal.mysql.*;
import net.mbi.wcloud.dispatch.solver.framework.lock.DistributedLock;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import net.mbi.wcloud.dispatch.solver.service.plan.model.*;
import net.mbi.wcloud.dispatch.solver.ortools.OrToolsSolverEngine;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSolveServiceImpl implements PlanSolveService {

    private final DistributedLock distributedLock;

    private final DispatchPlanMapper planMapper;
    private final DispatchTaskMapper taskMapper;
    private final DispatchVehicleMapper vehicleMapper;

    private final DispatchRouteMapper routeMapper;
    private final DispatchRouteStopMapper stopMapper;
    private final DispatchUnassignedMapper unassignedMapper;

    private final OrToolsSolverEngine solverEngine;

    @Override
    public void submitSolve(SolveRequestDTO req) {
        String lockKey = "solve:" + req.getTenantId() + ":" + req.getPlanId();

        log.info("SOLVE_SUBMIT tenantId={}, planId={}", req.getTenantId(), req.getPlanId());

        if (!distributedLock.tryLock(lockKey, 60)) {
            log.warn("SOLVE_REJECT_LOCKED tenantId={}, planId={}", req.getTenantId(), req.getPlanId());
            return;
        }

        // mark RUNNING
        markStatus(req.getTenantId(), req.getPlanId(), "RUNNING", "RUNNING");

        runAsync(req);
    }

    @Async("solveExecutor")
    public void runAsync(SolveRequestDTO req) {
        String lockKey = "solve:" + req.getTenantId() + ":" + req.getPlanId();
        long start = System.currentTimeMillis();

        log.info("SOLVE_START tenantId={}, planId={}, timeLimit={}s",
                req.getTenantId(), req.getPlanId(), req.getOptions().getTimeLimitSeconds());

        try {
            SolveInput input = assembleInput(req);
            SolveResult result = solverEngine.solve(req, input);

            long cost = System.currentTimeMillis() - start;
            result.getKpi().setSolveMillis(cost);

            persistResult(req.getTenantId(), req.getPlanId(), result);

            if ("SOLVED".equals(result.getStatus())) {
                markSolved(req.getTenantId(), req.getPlanId(), result);
            } else {
                markStatus(req.getTenantId(), req.getPlanId(), "FAILED", result.getMessage());
            }

            log.info("SOLVE_END tenantId={}, planId={}, status={}, cost={}ms, assigned={}, unassigned={}",
                    req.getTenantId(), req.getPlanId(), result.getStatus(), cost,
                    result.getKpi().getAssignedTaskCount(), result.getKpi().getUnassignedTaskCount());

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("SOLVE_FAIL tenantId={}, planId={}, cost={}ms, err={}",
                    req.getTenantId(), req.getPlanId(), cost, e.getMessage(), e);
            markStatus(req.getTenantId(), req.getPlanId(), "FAILED", e.getMessage());
        } finally {
            distributedLock.unlock(lockKey);
            log.info("SOLVE_UNLOCK tenantId={}, planId={}", req.getTenantId(), req.getPlanId());
        }
    }

    private SolveInput assembleInput(SolveRequestDTO req) {
        Long tenantId = req.getTenantId();
        Long planId = req.getPlanId();

        DispatchPlanDO plan = planMapper.selectOne(new LambdaQueryWrapper<DispatchPlanDO>()
                .eq(DispatchPlanDO::getId, planId)
                .eq(DispatchPlanDO::getTenantId, tenantId)
                .eq(DispatchPlanDO::getDeleted, 0));

        if (plan == null)
            throw new IllegalArgumentException("Plan not found");

        // vehicles
        List<DispatchVehicleDO> vehicleDOs = vehicleMapper.selectList(new LambdaQueryWrapper<DispatchVehicleDO>()
                .eq(DispatchVehicleDO::getTenantId, tenantId)
                .eq(DispatchVehicleDO::getDeleted, 0)
                .eq(DispatchVehicleDO::getStatus, "AVAILABLE")
                .in(req.getVehicleIds() != null && !req.getVehicleIds().isEmpty(), DispatchVehicleDO::getId,
                        req.getVehicleIds()));

        // tasks
        List<DispatchTaskDO> taskDOs = taskMapper.selectList(new LambdaQueryWrapper<DispatchTaskDO>()
                .eq(DispatchTaskDO::getTenantId, tenantId)
                .eq(DispatchTaskDO::getPlanId, planId)
                .eq(DispatchTaskDO::getDeleted, 0)
                .eq(DispatchTaskDO::getStatus, "WAITING")
                .in(req.getTaskIds() != null && !req.getTaskIds().isEmpty(), DispatchTaskDO::getId, req.getTaskIds()));

        if (vehicleDOs.isEmpty())
            throw new IllegalStateException("No available vehicles");
        if (taskDOs.isEmpty())
            throw new IllegalStateException("No waiting tasks");

        List<VehicleResource> vehicles = vehicleDOs.stream().map(v -> {
            VehicleResource vr = new VehicleResource();
            vr.setVehicleId(v.getId());
            vr.setStartNodeId(v.getStartNodeId());
            vr.setEndNodeId(v.getEndNodeId());
            vr.setCapacityWeight(v.getCapacityWeight() == null ? 0 : v.getCapacityWeight());
            vr.setWorkStartSec(v.getWorkStartSec() == null ? 0 : v.getWorkStartSec());
            vr.setWorkEndSec(v.getWorkEndSec() == null ? 24 * 3600 : v.getWorkEndSec());
            return vr;
        }).collect(Collectors.toList());

        List<TaskNode> tasks = taskDOs.stream().map(t -> {
            TaskNode tn = new TaskNode();
            tn.setTaskId(t.getId());
            tn.setNodeId(t.getNodeId());
            tn.setTwStartSec(t.getTwStartSec() == null ? 0 : t.getTwStartSec());
            tn.setTwEndSec(t.getTwEndSec() == null ? 24 * 3600 : t.getTwEndSec());
            tn.setServiceTimeSec(t.getServiceTimeSec() == null ? 0 : t.getServiceTimeSec());
            tn.setDemandWeight(t.getDemandWeight() == null ? 0 : t.getDemandWeight());
            return tn;
        }).collect(Collectors.toList());

        // Build node index: include depots + task nodes
        Set<Long> nodeIdSet = new LinkedHashSet<>();
        for (VehicleResource v : vehicles) {
            nodeIdSet.add(v.getStartNodeId());
            nodeIdSet.add(v.getEndNodeId());
        }
        for (TaskNode t : tasks)
            nodeIdSet.add(t.getNodeId());

        List<Long> indexToNodeId = new ArrayList<>(nodeIdSet);
        Map<Long, Integer> nodeIdToIndex = new HashMap<>();
        for (int i = 0; i < indexToNodeId.size(); i++)
            nodeIdToIndex.put(indexToNodeId.get(i), i);

        // Simple mock matrix: distance/time based on index diff (可替换百度矩阵)
        int n = indexToNodeId.size();
        long[][] dist = new long[n][n];
        long[][] time = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                long d = Math.abs(i - j) * 1000L;
                dist[i][j] = d;
                time[i][j] = Math.abs(i - j) * 120L;
            }
        }

        SolveInput input = new SolveInput();
        input.setTenantId(tenantId);
        input.setPlanId(planId);
        input.setPlan(plan);
        input.setVehicles(vehicles);
        input.setTasks(tasks);
        input.setIndexToNodeId(indexToNodeId);
        input.setNodeIdToIndex(nodeIdToIndex);

        MatrixData matrix = new MatrixData();
        matrix.setDistMeter(dist);
        matrix.setTimeSec(time);
        input.setMatrix(matrix);

        return input;
    }

    private void persistResult(Long tenantId, Long planId, SolveResult result) {
        // 1) logical delete old results
        routeMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DispatchRouteDO>()
                        .eq(DispatchRouteDO::getTenantId, tenantId)
                        .eq(DispatchRouteDO::getPlanId, planId)
                        .set(DispatchRouteDO::getDeleted, 1));

        stopMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DispatchRouteStopDO>()
                        .eq(DispatchRouteStopDO::getTenantId, tenantId)
                        .eq(DispatchRouteStopDO::getPlanId, planId)
                        .set(DispatchRouteStopDO::getDeleted, 1));

        unassignedMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DispatchUnassignedDO>()
                        .eq(DispatchUnassignedDO::getTenantId, tenantId)
                        .eq(DispatchUnassignedDO::getPlanId, planId)
                        .set(DispatchUnassignedDO::getDeleted, 1));

        // 2) insert new routes + stops
        for (SolveResult.RouteResult rr : result.getRoutes()) {
            DispatchRouteDO r = new DispatchRouteDO();
            r.setTenantId(tenantId);
            r.setPlanId(planId);
            r.setVehicleId(rr.getVehicleId());
            r.setTotalDistanceM(rr.getTotalDistanceM());
            r.setTotalTimeSec(rr.getTotalTimeSec());
            r.setDeleted(0);
            routeMapper.insert(r);

            Long routeId = r.getId();
            for (SolveResult.StopResult sr : rr.getStops()) {
                DispatchRouteStopDO s = new DispatchRouteStopDO();
                s.setTenantId(tenantId);
                s.setPlanId(planId);
                s.setRouteId(routeId);
                s.setSeq(sr.getSeq());
                s.setTaskId(sr.getTaskId());
                s.setNodeId(sr.getNodeId());
                s.setEtaSec(sr.getEtaSec());
                s.setEtdSec(sr.getEtdSec());
                s.setServiceTimeSec(sr.getServiceTimeSec());
                s.setDeleted(0);
                stopMapper.insert(s);
            }
        }

        // 3) insert unassigned
        for (SolveResult.UnassignedResult ur : result.getUnassigned()) {
            DispatchUnassignedDO u = new DispatchUnassignedDO();
            u.setTenantId(tenantId);
            u.setPlanId(planId);
            u.setTaskId(ur.getTaskId());
            u.setReasonCode(ur.getReasonCode());
            u.setDetail(ur.getDetail());
            u.setDeleted(0);
            unassignedMapper.insert(u);
        }
    }

    private void markSolved(Long tenantId, Long planId, SolveResult result) {
        DispatchPlanDO plan = planMapper.selectOne(new LambdaQueryWrapper<DispatchPlanDO>()
                .eq(DispatchPlanDO::getId, planId)
                .eq(DispatchPlanDO::getTenantId, tenantId)
                .eq(DispatchPlanDO::getDeleted, 0));

        if (plan == null)
            return;

        plan.setStatus("SOLVED");
        plan.setMessage("OK");
        plan.setAssignedCount(result.getKpi().getAssignedTaskCount());
        plan.setUnassignedCount(result.getKpi().getUnassignedTaskCount());
        plan.setSolveMillis(result.getKpi().getSolveMillis());
        planMapper.updateById(plan);
    }

    private void markStatus(Long tenantId, Long planId, String status, String message) {
        DispatchPlanDO plan = planMapper.selectOne(new LambdaQueryWrapper<DispatchPlanDO>()
                .eq(DispatchPlanDO::getId, planId)
                .eq(DispatchPlanDO::getTenantId, tenantId)
                .eq(DispatchPlanDO::getDeleted, 0));

        if (plan == null)
            return;

        plan.setStatus(status);
        plan.setMessage(message);
        planMapper.updateById(plan);
    }
}
