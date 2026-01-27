package net.mbi.wcloud.dispatch.solver.service.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(name = "SolveOptionsDTO", description = "调度求解参数选项")
public class SolveOptionsDTO {

    @Schema(description = "求解时间上限（秒），超过该时间求解器将主动停止", example = "60", minimum = "1", maximum = "300")
    @Min(1)
    @Max(300)
    private int timeLimitSeconds = 5;

    @Schema(description = "是否允许丢弃任务（true-允许，false-不允许）", example = "true")
    private boolean allowDrop = true;

    @Schema(description = "未分配任务惩罚值（目标函数权重，值越大越倾向于分配更多任务）", example = "10000", minimum = "0")
    @Min(0)
    private long unassignedPenalty = 10000;
}
