package net.mbi.wcloud.dispatch.solver.service.plan.model;

import lombok.Data;

@Data
public class VehicleResource {
    private Long vehicleId;
    private Long startNodeId;
    private Long endNodeId;

    private int capacityWeight;

    private int workStartSec;
    private int workEndSec;
}
