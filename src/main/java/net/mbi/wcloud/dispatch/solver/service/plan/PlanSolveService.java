package net.mbi.wcloud.dispatch.solver.service.plan;

import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;

public interface PlanSolveService {

    /**
     * 异步提交求解任务
     *
     * @return taskId
     */
    String submitSolve(SolveRequestDTO req);
}
