package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchPlanDO;

import java.time.LocalDateTime;

@Data
public class PlanVO {

    private Long id;
    private Long tenantId;
    private String planCode;

    private String status;
    private String message;

    private Integer timeLimitSec;
    private Long unassignedPenalty;
    private Integer allowDrop;

    private Long totalDistanceM;
    private Long totalTimeSec;
    private Integer assignedCount;
    private Integer unassignedCount;
    private Long solveMillis;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlanVO fromDO(DispatchPlanDO d) {
        PlanVO vo = new PlanVO();
        vo.setId(d.getId());
        vo.setTenantId(d.getTenantId());
        vo.setPlanCode(d.getPlanCode());
        vo.setStatus(d.getStatus());
        vo.setMessage(d.getMessage());
        vo.setTimeLimitSec(d.getTimeLimitSec());
        vo.setUnassignedPenalty(d.getUnassignedPenalty());
        vo.setAllowDrop(d.getAllowDrop());
        vo.setTotalDistanceM(d.getTotalDistanceM());
        vo.setTotalTimeSec(d.getTotalTimeSec());
        vo.setAssignedCount(d.getAssignedCount());
        vo.setUnassignedCount(d.getUnassignedCount());
        vo.setSolveMillis(d.getSolveMillis());
        vo.setCreatedAt(d.getCreateTime());
        vo.setUpdatedAt(d.getUpdateTime());
        return vo;
    }
}
