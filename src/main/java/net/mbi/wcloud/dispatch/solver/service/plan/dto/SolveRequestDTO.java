package net.mbi.wcloud.dispatch.solver.service.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SolveRequestDTO {

    @NotNull
    private Long tenantId;

    private Long planId;

    // 可选：指定部分任务/车辆参与求解
    private List<Long> taskIds;
    private List<Long> vehicleIds;

    @Valid
    @NotNull
    private SolveOptionsDTO options = new SolveOptionsDTO();
}
