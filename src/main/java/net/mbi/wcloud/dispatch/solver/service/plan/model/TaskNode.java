package net.mbi.wcloud.dispatch.solver.service.plan.model;

import lombok.Data;

@Data
public class TaskNode {
    private Long taskId;
    private Long nodeId;

    private int twStartSec;
    private int twEndSec;

    private int serviceTimeSec;

    private int demandWeight;
}
