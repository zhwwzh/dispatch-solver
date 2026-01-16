package net.mbi.wcloud.dispatch.solver.service.plan.model;

import lombok.Data;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchPlanDO;

import java.util.List;
import java.util.Map;

@Data
public class SolveInput {

    private Long tenantId;
    private Long planId;

    private DispatchPlanDO plan;

    private List<VehicleResource> vehicles;
    private List<TaskNode> tasks;

    private List<Long> indexToNodeId;
    private Map<Long, Integer> nodeIdToIndex;

    private MatrixData matrix;
}
