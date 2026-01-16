package net.mbi.wcloud.dispatch.solver.service.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SolveOptionsDTO {

    @Min(1)
    @Max(300)
    private int timeLimitSeconds = 5;

    private boolean allowDrop = true;

    @Min(0)
    private long unassignedPenalty = 10000;
}
