package net.mbi.wcloud.dispatch.solver.service.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanRouteVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanUnassignedVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanVO;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.*;
import net.mbi.wcloud.dispatch.solver.dal.mysql.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanQueryServiceImpl implements PlanQueryService {

    private final DispatchPlanMapper planMapper;
    private final DispatchRouteMapper routeMapper;
    private final DispatchRouteStopMapper stopMapper;
    private final DispatchUnassignedMapper unassignedMapper;

    @Override
    public PlanVO getPlan(Long tenantId, Long planId) {
        DispatchPlanDO plan = planMapper.selectOne(new LambdaQueryWrapper<DispatchPlanDO>()
                .eq(DispatchPlanDO::getId, planId)
                .eq(DispatchPlanDO::getTenantId, tenantId)
                .eq(DispatchPlanDO::getDeleted, 0));

        return plan == null ? null : PlanVO.fromDO(plan);
    }

    @Override
    public List<PlanRouteVO.Route> listRoutes(Long tenantId, Long planId) {
        List<DispatchRouteDO> routes = routeMapper.selectList(new LambdaQueryWrapper<DispatchRouteDO>()
                .eq(DispatchRouteDO::getTenantId, tenantId)
                .eq(DispatchRouteDO::getPlanId, planId)
                .eq(DispatchRouteDO::getDeleted, 0)
                .orderByAsc(DispatchRouteDO::getVehicleId));

        if (routes.isEmpty())
            return List.of();

        List<Long> routeIds = routes.stream().map(DispatchRouteDO::getId).collect(Collectors.toList());

        List<DispatchRouteStopDO> stops = stopMapper.selectList(new LambdaQueryWrapper<DispatchRouteStopDO>()
                .eq(DispatchRouteStopDO::getTenantId, tenantId)
                .eq(DispatchRouteStopDO::getPlanId, planId)
                .eq(DispatchRouteStopDO::getDeleted, 0)
                .in(DispatchRouteStopDO::getRouteId, routeIds)
                .orderByAsc(DispatchRouteStopDO::getRouteId)
                .orderByAsc(DispatchRouteStopDO::getSeq));

        Map<Long, List<DispatchRouteStopDO>> stopMap = stops.stream().collect(Collectors.groupingBy(
                DispatchRouteStopDO::getRouteId,
                LinkedHashMap::new,
                Collectors.toList()));

        List<PlanRouteVO.Route> out = new ArrayList<>();
        for (DispatchRouteDO r : routes) {
            PlanRouteVO.Route vo = PlanRouteVO.Route.fromDO(r);
            List<DispatchRouteStopDO> stopList = stopMap.getOrDefault(r.getId(), List.of());
            vo.setStops(stopList.stream().map(PlanRouteVO.Stop::fromDO).collect(Collectors.toList()));
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<PlanUnassignedVO.Item> listUnassigned(Long tenantId, Long planId) {
        List<DispatchUnassignedDO> list = unassignedMapper.selectList(new LambdaQueryWrapper<DispatchUnassignedDO>()
                .eq(DispatchUnassignedDO::getTenantId, tenantId)
                .eq(DispatchUnassignedDO::getPlanId, planId)
                .eq(DispatchUnassignedDO::getDeleted, 0)
                .orderByAsc(DispatchUnassignedDO::getId));

        return list.stream().map(PlanUnassignedVO.Item::fromDO).collect(Collectors.toList());
    }
}
