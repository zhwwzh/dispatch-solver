package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SolveTaskSubmitVO", description = "求解任务提交结果")
public class SolveTaskSubmitVO {

    @Schema(description = "求解任务ID", example = "task_20260122_0001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "任务初始状态（通常为 ACCEPTED）", example = "ACCEPTED")
    private String status;

    public SolveTaskSubmitVO() {
    }

    public SolveTaskSubmitVO(String taskId, String status) {
        this.taskId = taskId;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
