package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchPlanDO;

import java.time.LocalDateTime;

@Data
@Schema(name = "PlanVO", description = "调度方案信息及求解结果汇总")
public class PlanVO {

        @Schema(description = "方案ID", example = "10001")
        private Long id;

        @Schema(description = "租户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long tenantId;

        @Schema(description = "方案编码", example = "PLAN_20260122_0001")
        private String planCode;

        @Schema(description = "方案状态（如 CREATED / RUNNING / SUCCESS / FAILED）", example = "SUCCESS")
        private String status;

        @Schema(description = "方案状态说明或失败原因", example = "Solve completed successfully")
        private String message;

        @Schema(description = "求解时间限制（秒）", example = "60")
        private Integer timeLimitSec;

        @Schema(description = "未分配惩罚值（用于目标函数权重）", example = "100000")
        private Long unassignedPenalty;

        @Schema(description = "是否允许丢弃任务（0-不允许，1-允许）", example = "0", allowableValues = { "0", "1" })
        private Integer allowDrop;

        @Schema(description = "总里程（米）", example = "125430")
        private Long totalDistanceM;

        @Schema(description = "总耗时（秒）", example = "28400")
        private Long totalTimeSec;

        @Schema(description = "已分配任务数", example = "120")
        private Integer assignedCount;

        @Schema(description = "未分配任务数", example = "5")
        private Integer unassignedCount;

        @Schema(description = "求解耗时（毫秒）", example = "15234")
        private Long solveMillis;

        @Schema(description = "创建时间", example = "2026-01-22T10:15:30")
        private LocalDateTime createdAt;

        @Schema(description = "更新时间", example = "2026-01-22T10:16:05")
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
