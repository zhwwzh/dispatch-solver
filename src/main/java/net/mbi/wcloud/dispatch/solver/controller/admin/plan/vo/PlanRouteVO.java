package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchRouteDO;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchRouteStopDO;

import java.util.ArrayList;
import java.util.List;

public class PlanRouteVO {

    @Data
    @Schema(name = "PlanRouteVO.Route", description = "线路信息（车辆维度的路径/统计）")
    public static class Route {

        @Schema(description = "线路ID", example = "20001")
        private Long id;

        @Schema(description = "方案ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long planId;

        @Schema(description = "车辆ID", example = "30001", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long vehicleId;

        @Schema(description = "线路总里程（米）", example = "15230")
        private Long totalDistanceM;

        @Schema(description = "线路总耗时（秒）", example = "3720")
        private Long totalTimeSec;

        @Schema(description = "停靠点列表（按 seq 升序）")
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
    @Schema(name = "PlanRouteVO.Stop", description = "线路停靠点（车辆路线上的一个节点/任务点）")
    public static class Stop {

        @Schema(description = "停靠点ID", example = "50001")
        private Long id;

        @Schema(description = "停靠点序号（从 0/1 开始，取决于你的业务定义）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer seq;

        @Schema(description = "任务ID（若该停靠点对应业务任务）", example = "90001")
        private Long taskId;

        @Schema(description = "节点ID（网点/客户点/仓库点等）", example = "80001", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long nodeId;

        @Schema(description = "预计到达时间（秒，通常为相对时间或时间轴偏移）", example = "1200")
        private Long etaSec;

        @Schema(description = "预计离开时间（秒，通常为相对时间或时间轴偏移）", example = "1500")
        private Long etdSec;

        @Schema(description = "服务时长（秒）", example = "300")
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
