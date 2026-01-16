package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchUnassignedDO;

public class PlanUnassignedVO {

    @Data
    public static class Item {
        private Long id;
        private Long planId;
        private Long taskId;
        private String reasonCode;
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
