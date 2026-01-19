package net.mbi.wcloud.dispatch.solver.service.plan.model;

/**
 * 求解任务状态
 */
public enum SolveTaskStatus {

    ACCEPTED("ACCEPTED"),
    RUNNING("RUNNING"),
    SOLVED("SOLVED"),
    FAILED("FAILED");

    private final String code;

    SolveTaskStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
