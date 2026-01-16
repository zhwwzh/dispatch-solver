package net.mbi.wcloud.dispatch.solver.service.plan;

import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;

public interface PlanSolveService {

    void submitSolve(SolveRequestDTO req);
}
