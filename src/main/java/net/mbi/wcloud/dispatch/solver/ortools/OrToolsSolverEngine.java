package net.mbi.wcloud.dispatch.solver.ortools;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import net.mbi.wcloud.dispatch.solver.service.plan.model.MatrixData;
import net.mbi.wcloud.dispatch.solver.service.plan.model.SolveInput;
import net.mbi.wcloud.dispatch.solver.service.plan.model.SolveResult;
import net.mbi.wcloud.dispatch.solver.service.plan.model.TaskNode;
import net.mbi.wcloud.dispatch.solver.service.plan.model.VehicleResource;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OrToolsSolverEngine {

    public SolveResult solve(SolveRequestDTO req, SolveInput in) {
        Loader.loadNativeLibraries();

        SolveResult out = new SolveResult();

        List<TaskNode> tasks = in.getTasks();
        List<VehicleResource> vehicles = in.getVehicles();
        MatrixData matrix = in.getMatrix();

        int taskCount = tasks == null ? 0 : tasks.size();
        int vehicleCount = vehicles == null ? 0 : vehicles.size();
        int nodeCount = in.getIndexToNodeId() == null ? 0 : in.getIndexToNodeId().size();

        log.info("ORTOOLS_START planId={}, tasks={}, vehicles={}, nodes={}, timeLimit={}s",
                req.getPlanId(), taskCount, vehicleCount, nodeCount, req.getOptions().getTimeLimitSeconds());

        if (taskCount == 0 || vehicleCount == 0) {
            out.setStatus("FAILED");
            out.setMessage("No tasks or vehicles");
            return out;
        }

        int[] starts = new int[vehicleCount];
        int[] ends = new int[vehicleCount];

        for (int v = 0; v < vehicleCount; v++) {
            starts[v] = in.getNodeIdToIndex().get(vehicles.get(v).getStartNodeId());
            ends[v] = in.getNodeIdToIndex().get(vehicles.get(v).getEndNodeId());
        }

        RoutingIndexManager manager = new RoutingIndexManager(nodeCount, vehicleCount, starts, ends);
        RoutingModel routing = new RoutingModel(manager);

        // Distance cost
        int distCb = routing.registerTransitCallback((long fromIdx, long toIdx) -> {
            int fromNode = manager.indexToNode(fromIdx);
            int toNode = manager.indexToNode(toIdx);
            return matrix.getDistMeter()[fromNode][toNode];
        });
        routing.setArcCostEvaluatorOfAllVehicles(distCb);

        // Demand
        int demandCb = routing.registerUnaryTransitCallback((long fromIdx) -> {
            int fromNode = manager.indexToNode(fromIdx);
            Long nodeId = in.getIndexToNodeId().get(fromNode);
            TaskNode t = findTaskByNodeId(tasks, nodeId);
            return t == null ? 0 : t.getDemandWeight();
        });

        long[] caps = new long[vehicleCount];
        for (int v = 0; v < vehicleCount; v++)
            caps[v] = vehicles.get(v).getCapacityWeight();

        routing.addDimensionWithVehicleCapacity(demandCb, 0, caps, true, "Capacity");

        // Time: travel + service
        int timeCb = routing.registerTransitCallback((long fromIdx, long toIdx) -> {
            int fromNode = manager.indexToNode(fromIdx);
            int toNode = manager.indexToNode(toIdx);

            long travel = matrix.getTimeSec()[fromNode][toNode];
            Long fromNodeId = in.getIndexToNodeId().get(fromNode);
            TaskNode t = findTaskByNodeId(tasks, fromNodeId);
            long service = (t == null) ? 0 : t.getServiceTimeSec();

            return travel + service;
        });

        routing.addDimension(timeCb, 30 * 60, 24 * 3600, false, "Time");
        RoutingDimension timeDim = routing.getMutableDimension("Time");

        // Task time windows
        for (TaskNode t : tasks) {
            Integer nodeIdx = in.getNodeIdToIndex().get(t.getNodeId());
            if (nodeIdx == null)
                continue;
            long idx = manager.nodeToIndex(nodeIdx);
            timeDim.cumulVar(idx).setRange(t.getTwStartSec(), t.getTwEndSec());
        }

        // Vehicle shifts
        for (int v = 0; v < vehicleCount; v++) {
            VehicleResource vr = vehicles.get(v);
            timeDim.cumulVar(routing.start(v)).setRange(vr.getWorkStartSec(), vr.getWorkEndSec());
            timeDim.cumulVar(routing.end(v)).setRange(vr.getWorkStartSec(), vr.getWorkEndSec());
        }

        // Allow drop
        if (req.getOptions().isAllowDrop()) {
            long penalty = req.getOptions().getUnassignedPenalty();
            for (TaskNode t : tasks) {
                Integer nodeIdx = in.getNodeIdToIndex().get(t.getNodeId());
                if (nodeIdx == null)
                    continue;
                long idx = manager.nodeToIndex(nodeIdx);
                routing.addDisjunction(new long[] { idx }, penalty);
            }
        }

        RoutingSearchParameters search = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PARALLEL_CHEAPEST_INSERTION)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                        .setSeconds(req.getOptions().getTimeLimitSeconds()).build())
                .build();

        long t0 = System.currentTimeMillis();
        Assignment solution = routing.solveWithParameters(search);
        long solveCost = System.currentTimeMillis() - t0;

        if (solution == null) {
            out.setStatus("FAILED");
            out.setMessage("No solution");
            for (TaskNode t : tasks) {
                SolveResult.UnassignedResult u = new SolveResult.UnassignedResult();
                u.setTaskId(t.getTaskId());
                u.setReasonCode("NO_SOLUTION");
                u.setDetail("No solution found");
                out.getUnassigned().add(u);
            }
            out.getKpi().setAssignedTaskCount(0);
            out.getKpi().setUnassignedTaskCount(tasks.size());
            log.warn("ORTOOLS_END planId={}, solved=false, cost={}ms", req.getPlanId(), solveCost);
            return out;
        }

        out.setStatus("SOLVED");
        out.setMessage("OK");

        Set<Long> assignedTaskIds = new HashSet<>();

        for (int v = 0; v < vehicleCount; v++) {
            SolveResult.RouteResult rr = new SolveResult.RouteResult();
            rr.setVehicleId(vehicles.get(v).getVehicleId());

            long idx = routing.start(v);
            int seq = 0;

            while (!routing.isEnd(idx)) {
                int node = manager.indexToNode(idx);
                Long nodeId = in.getIndexToNodeId().get(node);

                TaskNode task = findTaskByNodeId(tasks, nodeId);
                if (task != null) {
                    long eta = solution.min(timeDim.cumulVar(idx));

                    SolveResult.StopResult sr = new SolveResult.StopResult();
                    sr.setSeq(seq++);
                    sr.setTaskId(task.getTaskId());
                    sr.setNodeId(task.getNodeId());
                    sr.setEtaSec(eta);
                    sr.setServiceTimeSec(task.getServiceTimeSec());
                    sr.setEtdSec(eta + task.getServiceTimeSec());

                    rr.getStops().add(sr);
                    assignedTaskIds.add(task.getTaskId());
                }

                idx = solution.value(routing.nextVar(idx));
            }

            out.getRoutes().add(rr);
        }

        for (TaskNode t : tasks) {
            if (!assignedTaskIds.contains(t.getTaskId())) {
                SolveResult.UnassignedResult u = new SolveResult.UnassignedResult();
                u.setTaskId(t.getTaskId());
                u.setReasonCode("DROPPED");
                u.setDetail("Dropped by penalty");
                out.getUnassigned().add(u);
            }
        }

        out.getKpi().setAssignedTaskCount(assignedTaskIds.size());
        out.getKpi().setUnassignedTaskCount(out.getUnassigned().size());

        log.info("ORTOOLS_END planId={}, solved=true, cost={}ms, routes={}, assigned={}, unassigned={}",
                req.getPlanId(), solveCost, out.getRoutes().size(),
                out.getKpi().getAssignedTaskCount(), out.getKpi().getUnassignedTaskCount());

        return out;
    }

    private TaskNode findTaskByNodeId(List<TaskNode> tasks, Long nodeId) {
        for (TaskNode t : tasks) {
            if (Objects.equals(t.getNodeId(), nodeId))
                return t;
        }
        return null;
    }
}
