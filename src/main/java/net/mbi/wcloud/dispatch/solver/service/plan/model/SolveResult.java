package net.mbi.wcloud.dispatch.solver.service.plan.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SolveResult {

    private String status; // SOLVED/FAILED
    private String message;

    private KPI kpi = new KPI();

    private List<RouteResult> routes = new ArrayList<>();
    private List<UnassignedResult> unassigned = new ArrayList<>();

    @Data
    public static class KPI {
        private int assignedTaskCount;
        private int unassignedTaskCount;
        private long solveMillis;
    }

    @Data
    public static class RouteResult {
        private Long vehicleId;
        private long totalDistanceM;
        private long totalTimeSec;
        private List<StopResult> stops = new ArrayList<>();
    }

    @Data
    public static class StopResult {
        private int seq;
        private Long taskId;
        private Long nodeId;
        private long etaSec;
        private long etdSec;
        private int serviceTimeSec;
    }

    @Data
    public static class UnassignedResult {
        private Long taskId;
        private String reasonCode;
        private String detail;
    }
}
