package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchUnassignedDO;

public class PlanUnassignedVO {

    @Data
    @Schema(name = "PlanUnassignedVO.Item", description = "方案中未被分配的任务/订单信息")
    public static class Item {

        @Schema(description = "记录ID", example = "60001")
        private Long id;

        @Schema(description = "方案ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long planId;

        @Schema(description = "任务ID（未被分配的业务任务/订单）", example = "90001", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long taskId;

        @Schema(description = "未分配原因码（业务枚举值，如 TIME_WINDOW_CONFLICT / NO_CAPACITY / DISTANCE_LIMIT）", example = "TIME_WINDOW_CONFLICT")
        private String reasonCode;

        @Schema(description = "未分配详细说明（可读文本，用于解释具体原因）", example = "任务时间窗与车辆可用时间冲突")
        private String detail;

        public static Item fromDO(DispatchUnassignedDO d) {
            Item i = new Item();
            i.setId(d.getId());
            i.setPlanId(d.getPlanId());
            i.setTaskId(d.getTaskId());
            i.setReasonCode(d.getReasonCode());
            i.setDetail(d.getDetail());
            return i;
        }
    }
}
