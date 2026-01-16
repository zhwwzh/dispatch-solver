package net.mbi.wcloud.dispatch.solver.controller.admin.plan;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanRouteVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanUnassignedVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanVO;
import net.mbi.wcloud.dispatch.solver.service.plan.PlanQueryService;
import net.mbi.wcloud.dispatch.solver.service.plan.PlanSolveService;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
public class PlanController {

    private final PlanSolveService planSolveService;
    private final PlanQueryService planQueryService;

    /**
     * 触发异步求解
     */
    @PostMapping("/{planId}/solve")
    public String solve(@PathVariable Long planId, @RequestBody @Valid SolveRequestDTO req) {
        req.setPlanId(planId);
        log.info("HTTP_SOLVE planId={}, tenantId={}", planId, req.getTenantId());
        planSolveService.submitSolve(req);
        return "ACCEPTED";
    }

    /**
     * 查询方案状态 + KPI
     */
    @GetMapping("/{planId}")
    public PlanVO getPlan(@PathVariable Long planId, @RequestParam("tenantId") Long tenantId) {
        return planQueryService.getPlan(tenantId, planId);
    }

    /**
     * 查询线路（含 stops）
     */
    @GetMapping("/{planId}/routes")
    public List<PlanRouteVO.Route> getRoutes(@PathVariable Long planId, @RequestParam("tenantId") Long tenantId) {
        return planQueryService.listRoutes(tenantId, planId);
    }

    /**
     * 查询未分配
     */
    @GetMapping("/{planId}/unassigned")
    public List<PlanUnassignedVO.Item> getUnassigned(@PathVariable Long planId,
            @RequestParam("tenantId") Long tenantId) {
        return planQueryService.listUnassigned(tenantId, planId);
    }
}
