package net.mbi.wcloud.dispatch.solver.service.plan;

import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanRouteVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanUnassignedVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanVO;

import java.util.List;

public interface PlanQueryService {

    PlanVO getPlan(Long tenantId, Long planId);

    List<PlanRouteVO.Route> listRoutes(Long tenantId, Long planId);

    List<PlanUnassignedVO.Item> listUnassigned(Long tenantId, Long planId);
}
