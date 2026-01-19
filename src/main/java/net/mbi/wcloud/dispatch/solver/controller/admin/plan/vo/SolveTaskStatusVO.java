package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

public class SolveTaskStatusVO {

    private String taskId;
    private String status;
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
