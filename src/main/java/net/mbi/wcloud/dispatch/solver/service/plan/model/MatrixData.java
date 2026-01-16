package net.mbi.wcloud.dispatch.solver.service.plan.model;

import lombok.Data;

@Data
public class MatrixData {
    private long[][] distMeter;
    private long[][] timeSec;
}
