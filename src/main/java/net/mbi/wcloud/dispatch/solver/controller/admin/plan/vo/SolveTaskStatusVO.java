package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SolveTaskStatusVO", description = "求解任务状态信息")
public class SolveTaskStatusVO {

    @Schema(description = "求解任务ID", example = "task_20260122_0001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "任务状态（如 ACCEPTED / RUNNING / SUCCESS / FAILED）", example = "RUNNING")
    private String status;

    @Schema(description = "任务状态说明或失败原因", example = "Solver is running")
    private String message;

    public SolveTaskStatusVO() {
    }

    public SolveTaskStatusVO(String taskId, String status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
