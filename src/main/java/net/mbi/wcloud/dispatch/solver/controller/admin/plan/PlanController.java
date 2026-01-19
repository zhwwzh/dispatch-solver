package net.mbi.wcloud.dispatch.solver.controller.admin.plan;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanRouteVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanUnassignedVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.PlanVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.SolveTaskStatusVO;
import net.mbi.wcloud.dispatch.solver.controller.admin.plan.vo.SolveTaskSubmitVO;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchSolveJobDO;
import net.mbi.wcloud.dispatch.solver.dal.mysql.DispatchSolveJobMapper;
import net.mbi.wcloud.dispatch.solver.framework.common.pojo.CommonResult;
import net.mbi.wcloud.dispatch.solver.service.plan.PlanQueryService;
import net.mbi.wcloud.dispatch.solver.service.plan.PlanSolveService;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import net.mbi.wcloud.dispatch.solver.service.plan.model.SolveTaskStatus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
public class PlanController {

    private final PlanSolveService planSolveService;
    private final PlanQueryService planQueryService;
    private final DispatchSolveJobMapper solveJobMapper;

    /**
     * 执行异步求解
     */

    @PostMapping("/{planId}/solve")
    public ResponseEntity<CommonResult<SolveTaskSubmitVO>> solve(@PathVariable Long planId,
            @RequestBody @Valid SolveRequestDTO req) {
        req.setPlanId(planId);
        log.info("HTTP_SOLVE planId={}, tenantId={}", planId, req.getTenantId());

        String taskId = planSolveService.submitSolve(req);

        SolveTaskSubmitVO vo = new SolveTaskSubmitVO(taskId, SolveTaskStatus.ACCEPTED.code());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CommonResult.success(vo));
    }

    /**
     * 查询方案详细信息
     */
    @GetMapping("/{planId}")
    public PlanVO getPlan(@PathVariable Long planId, @RequestParam("tenantId") Long tenantId) {
        return planQueryService.getPlan(tenantId, planId);
    }

    /**
     * 查询线路详细信息
     */
    @GetMapping("/{planId}/routes")
    public List<PlanRouteVO.Route> getRoutes(@PathVariable Long planId, @RequestParam("tenantId") Long tenantId) {
        return planQueryService.listRoutes(tenantId, planId);
    }

    /**
     * 查询未分配详细信息
     */
    @GetMapping("/{planId}/unassigned")
    public List<PlanUnassignedVO.Item> getUnassigned(@PathVariable Long planId,
            @RequestParam("tenantId") Long tenantId) {
        return planQueryService.listUnassigned(tenantId, planId);
    }

    /**
     * 查询任务详细信息
     */
    @GetMapping("/{planId}/solve/{taskId}")
    public CommonResult<SolveTaskStatusVO> getSolveTask(@PathVariable Long planId,
            @PathVariable String taskId,
            @RequestParam("tenantId") Long tenantId) {
        DispatchSolveJobDO job = solveJobMapper.selectOne(new LambdaQueryWrapper<DispatchSolveJobDO>()
                .eq(DispatchSolveJobDO::getTenantId, tenantId)
                .eq(DispatchSolveJobDO::getPlanId, planId)
                .eq(DispatchSolveJobDO::getTaskId, taskId)
                .eq(DispatchSolveJobDO::getDeleted, 0));

        if (job == null) {
            // ？？？？你若已接统一异常体系，建议抛 ServiceException(404, "Task not found")
            return CommonResult.error(404, "Solve task not found");
        }

        return CommonResult.success(new SolveTaskStatusVO(job.getTaskId(), job.getStatus(), job.getMessage()));
    }
}
