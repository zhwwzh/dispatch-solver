package net.mbi.wcloud.dispatch.solver.service.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "SolveRequestDTO", description = "调度方案求解请求参数")
public class SolveRequestDTO {

    @Schema(description = "租户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long tenantId;

    @Schema(description = "方案ID（通常由路径 /plans/{planId}/solve 注入，不建议前端手动传）", example = "10001")
    private Long planId;

    @Schema(description = "指定参与求解的任务ID列表（为空表示全部任务参与求解）", example = "[90001,90002]")
    private List<Long> taskIds;

    @Schema(description = "指定参与求解的车辆ID列表（为空表示全部车辆参与求解）", example = "[30001,30002]")
    private List<Long> vehicleIds;

    @Schema(description = "求解参数选项（时间限制、惩罚系数、策略开关等）", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private SolveOptionsDTO options = new SolveOptionsDTO();
}
