package net.mbi.wcloud.dispatch.solver.service.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.*;
import net.mbi.wcloud.dispatch.solver.dal.mysql.*;
import net.mbi.wcloud.dispatch.solver.service.plan.model.*;
import net.mbi.wcloud.dispatch.solver.framework.lock.DistributedLock;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import net.mbi.wcloud.dispatch.solver.ortools.OrToolsSolverEngine;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final DispatchSolveJobMapper solveJobMapper;
    private final OrToolsSolverEngine solverEngine;

    @Override
    public String submitSolve(SolveRequestDTO req) {
        Long tenantId = req.getTenantId();
        Long planId = req.getPlanId();
        String lockKey = "solve:" + tenantId + ":" + planId;

        log.info("SOLVE_SUBMIT tenantId={}, planId={}", tenantId, planId);

        // 1. 先查活跃任务（ACCEPTED/RUNNING）：幂等命中直接返回同一个 taskId
        DispatchSolveJobDO active = findActiveJob(tenantId, planId);
        if (active != null) {
            log.info("SOLVE_IDEMPOTENT_HIT tenantId={}, planId={}, taskId={}, status={}",
                    tenantId, planId, active.getTaskId(), active.getStatus());
            return active.getTaskId();
        }

        // 2. 不存在活跃任务 -> 尝试加锁
        if (!distributedLock.tryLock(lockKey, 60)) {
            log.warn("SOLVE_LOCK_BUSY tenantId={}, planId={}", tenantId, planId);

            // 3. 加锁失败，再查一次（防并发空窗）：如果别人刚创建了任务，这里能拿到同一个 taskId
            active = findActiveJob(tenantId, planId);
            if (active != null) {
                log.info("SOLVE_IDEMPOTENT_AFTER_LOCK_FAIL tenantId={}, planId={}, taskId={}, status={}",
                        tenantId, planId, active.getTaskId(), active.getStatus());
                return active.getTaskId();
            }

            // 4. 极低概率：锁忙但又查不到活跃任务 -> 提示重试（也可按你项目异常体系抛 ServiceException）
            throw new IllegalStateException("Solve submit busy, please retry");
        }

        // 5. 获取锁成功：创建新任务
        String taskId = "solve-" + tenantId + "-" + planId + "-" + System.currentTimeMillis();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        DispatchSolveJobDO job = new DispatchSolveJobDO();
        job.setTenantId(tenantId);
        job.setPlanId(planId);
        job.setTaskId(taskId);
        job.setStatus(SolveTaskStatus.ACCEPTED.code());
        job.setMessage(SolveTaskStatus.ACCEPTED.code());
        job.setCreateTime(now);
        job.setUpdateTime(now);
        job.setDeleted(0);
        solveJobMapper.insert(job);

        markStatus(tenantId, planId,
                SolveTaskStatus.ACCEPTED.code(),
                SolveTaskStatus.ACCEPTED.code());

        runAsync(req, taskId);

        return taskId;
    }

    @Async("solveExecutor")
    public void runAsync(SolveRequestDTO req, String taskId) {
        String lockKey = "solve:" + req.getTenantId() + ":" + req.getPlanId();
        long start = System.currentTimeMillis();

        log.info("SOLVE_START tenantId={}, planId={}, taskId={}, timeLimit={}s",
                req.getTenantId(), req.getPlanId(), taskId, req.getOptions().getTimeLimitSeconds());

        // ✅ 真正开始执行时：RUNNING
        markStatus(req.getTenantId(), req.getPlanId(),
                SolveTaskStatus.RUNNING.code(),
                SolveTaskStatus.RUNNING.code());
        updateJobStatus(req.getTenantId(), req.getPlanId(), taskId,
                SolveTaskStatus.RUNNING.code(), SolveTaskStatus.RUNNING.code());

        try {
            SolveInput input = assembleInput(req);
            SolveResult result = solverEngine.solve(req, input);

            long cost = System.currentTimeMillis() - start;
            result.getKpi().setSolveMillis(cost);

            persistResult(req.getTenantId(), req.getPlanId(), result);

            if (SolveTaskStatus.SOLVED.code().equals(result.getStatus())) {
                markSolved(req.getTenantId(), req.getPlanId(), result);
                updateJobStatus(req.getTenantId(), req.getPlanId(), taskId,
                        SolveTaskStatus.SOLVED.code(), "OK");
            } else {
                markStatus(req.getTenantId(), req.getPlanId(),
                        SolveTaskStatus.FAILED.code(), result.getMessage());
                updateJobStatus(req.getTenantId(), req.getPlanId(), taskId,
                        SolveTaskStatus.FAILED.code(), result.getMessage());
            }

            log.info("SOLVE_END tenantId={}, planId={}, taskId={}, status={}, cost={}ms, assigned={}, unassigned={}",
                    req.getTenantId(), req.getPlanId(), taskId, result.getStatus(), cost,
                    result.getKpi().getAssignedTaskCount(), result.getKpi().getUnassignedTaskCount());

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("SOLVE_FAIL tenantId={}, planId={}, taskId={}, cost={}ms, err={}",
                    req.getTenantId(), req.getPlanId(), taskId, cost, e.getMessage(), e);

            markStatus(req.getTenantId(), req.getPlanId(),
                    SolveTaskStatus.FAILED.code(), e.getMessage());
            updateJobStatus(req.getTenantId(), req.getPlanId(), taskId,
                    SolveTaskStatus.FAILED.code(), e.getMessage());
        } finally {
            distributedLock.unlock(lockKey);
            log.info("SOLVE_UNLOCK tenantId={}, planId={}, taskId={}", req.getTenantId(), req.getPlanId(), taskId);
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

        plan.setStatus(SolveTaskStatus.SOLVED.code());
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

    private void updateJobStatus(Long tenantId, Long planId, String taskId, String status, String message) {
        LocalDateTime now = LocalDateTime.now();
        solveJobMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DispatchSolveJobDO>()
                        .eq(DispatchSolveJobDO::getTenantId, tenantId)
                        .eq(DispatchSolveJobDO::getPlanId, planId)
                        .eq(DispatchSolveJobDO::getTaskId, taskId)
                        .eq(DispatchSolveJobDO::getDeleted, 0)
                        .set(DispatchSolveJobDO::getStatus, status)
                        .set(DispatchSolveJobDO::getMessage, message)
                        .set(DispatchSolveJobDO::getUpdateTime, now));
    }

    private DispatchSolveJobDO findActiveJob(Long tenantId, Long planId) {
        return solveJobMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DispatchSolveJobDO>()
                        .eq(DispatchSolveJobDO::getTenantId, tenantId)
                        .eq(DispatchSolveJobDO::getPlanId, planId)
                        .eq(DispatchSolveJobDO::getDeleted, 0)
                        .in(DispatchSolveJobDO::getStatus,
                                SolveTaskStatus.ACCEPTED.code(),
                                SolveTaskStatus.RUNNING.code())
                        .orderByDesc(DispatchSolveJobDO::getUpdateTime)
                        .last("limit 1"));
    }
}
