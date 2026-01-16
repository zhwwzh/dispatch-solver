package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchRouteDO;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchRouteStopDO;

import java.util.ArrayList;
import java.util.List;

public class PlanRouteVO {

    @Data
    public static class Route {
        private Long id;
        private Long planId;
        private Long vehicleId;
        private Long totalDistanceM;
        private Long totalTimeSec;

        private List<Stop> stops = new ArrayList<>();

        public static Route fromDO(DispatchRouteDO d) {
            Route r = new Route();
            r.setId(d.getId());
            r.setPlanId(d.getPlanId());
            r.setVehicleId(d.getVehicleId());
            r.setTotalDistanceM(d.getTotalDistanceM());
            r.setTotalTimeSec(d.getTotalTimeSec());
            return r;
        }
    }

    @Data
    public static class Stop {
        private Long id;
        private Integer seq;
        private Long taskId;
        private Long nodeId;
        private Long etaSec;
        private Long etdSec;
        private Integer serviceTimeSec;

        public static Stop fromDO(DispatchRouteStopDO d) {
            Stop s = new Stop();
            s.setId(d.getId());
            s.setSeq(d.getSeq());
            s.setTaskId(d.getTaskId());
            s.setNodeId(d.getNodeId());
            s.setEtaSec(d.getEtaSec());
            s.setEtdSec(d.getEtdSec());
            s.setServiceTimeSec(d.getServiceTimeSec());
            return s;
        }
    }
}
