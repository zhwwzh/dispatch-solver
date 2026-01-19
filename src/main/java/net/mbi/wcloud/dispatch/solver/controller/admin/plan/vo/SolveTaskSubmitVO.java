package net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo;

public class SolveTaskSubmitVO {

    private String taskId;
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
