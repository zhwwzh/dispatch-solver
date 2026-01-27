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
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
@Tag(name = "管理端-调度方案 Plan", description = "调度方案查询、求解任务提交与执行状态查询")
public class PlanController {

        private final PlanSolveService planSolveService;
        private final PlanQueryService planQueryService;
        private final DispatchSolveJobMapper solveJobMapper;

        /**
         * 提交异步求解任务
         */
        @PostMapping("/{planId}/solve")
        @Operation(summary = "提交方案求解任务", description = """
                        提交调度方案的异步求解任务。

                        特性说明：
                        - 异步执行，立即返回 taskId
                        - 建议幂等：同一 plan 在 RUNNING 状态下重复提交，应返回同一个 taskId
                        - 任务执行状态需通过「查询求解任务状态」接口获取
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "提交成功，返回求解任务信息", content = @Content(schema = @Schema(implementation = CommonResult.class))),
                        @ApiResponse(responseCode = "400", description = "请求参数不合法"),
                        @ApiResponse(responseCode = "401", description = "未认证或 Token 无效"),
                        @ApiResponse(responseCode = "403", description = "无权限访问")
        })
        public CommonResult<SolveTaskSubmitVO> solve(
                        @Parameter(description = "方案ID", required = true, example = "10001") @PathVariable Long planId,

                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "求解参数", required = true) @RequestBody @Valid SolveRequestDTO req) {

                req.setPlanId(planId);
                log.info("HTTP_SOLVE planId={}, tenantId={}", planId, req.getTenantId());

                String taskId = planSolveService.submitSolve(req);
                SolveTaskSubmitVO vo = new SolveTaskSubmitVO(taskId, SolveTaskStatus.ACCEPTED.code());

                return CommonResult.success(vo);
        }

        /**
         * 查询方案详情
         */
        @GetMapping("/{planId}")
        @Operation(summary = "查询方案详情", description = "根据租户ID与方案ID查询调度方案的基础信息及求解结果汇总")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = CommonResult.class))),
                        @ApiResponse(responseCode = "404", description = "方案不存在"),
                        @ApiResponse(responseCode = "401", description = "未认证或 Token 无效")
        })
        public CommonResult<PlanVO> getPlan(
                        @Parameter(description = "方案ID", required = true, example = "10001") @PathVariable Long planId,

                        @Parameter(description = "租户ID", required = true, example = "1") @RequestParam("tenantId") Long tenantId) {

                return CommonResult.success(
                                planQueryService.getPlan(tenantId, planId));
        }

        /**
         * 查询线路明细
         */
        @GetMapping("/{planId}/routes")
        @Operation(summary = "查询方案线路明细", description = "查询指定方案下的车辆线路信息（按车辆维度）")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = CommonResult.class))),
                        @ApiResponse(responseCode = "404", description = "方案不存在"),
                        @ApiResponse(responseCode = "401", description = "未认证或 Token 无效")
        })
        public CommonResult<List<PlanRouteVO.Route>> getRoutes(
                        @Parameter(description = "方案ID", required = true, example = "10001") @PathVariable Long planId,

                        @Parameter(description = "租户ID", required = true, example = "1") @RequestParam("tenantId") Long tenantId) {

                return CommonResult.success(
                                planQueryService.listRoutes(tenantId, planId));
        }

        /**
         * 查询未分配任务明细
         */
        @GetMapping("/{planId}/unassigned")
        @Operation(summary = "查询未分配任务明细", description = "查询调度方案中因约束冲突等原因未被分配的任务/订单")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = CommonResult.class))),
                        @ApiResponse(responseCode = "404", description = "方案不存在"),
                        @ApiResponse(responseCode = "401", description = "未认证或 Token 无效")
        })
        public CommonResult<List<PlanUnassignedVO.Item>> getUnassigned(
                        @Parameter(description = "方案ID", required = true, example = "10001") @PathVariable Long planId,

                        @Parameter(description = "租户ID", required = true, example = "1") @RequestParam("tenantId") Long tenantId) {

                return CommonResult.success(
                                planQueryService.listUnassigned(tenantId, planId));
        }

        /**
         * 查询求解任务状态
         */
        @GetMapping("/{planId}/solve/{taskId}")
        @Operation(summary = "查询求解任务状态", description = "根据方案ID与任务ID查询调度求解任务的当前执行状态")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = CommonResult.class))),
                        @ApiResponse(responseCode = "404", description = "任务不存在"),
                        @ApiResponse(responseCode = "401", description = "未认证或 Token 无效")
        })
        public CommonResult<SolveTaskStatusVO> getSolveTask(
                        @Parameter(description = "方案ID", required = true, example = "10001") @PathVariable Long planId,

                        @Parameter(description = "任务ID", required = true, example = "task_20260122_0001") @PathVariable String taskId,

                        @Parameter(description = "租户ID", required = true, example = "1") @RequestParam("tenantId") Long tenantId) {

                DispatchSolveJobDO job = solveJobMapper.selectOne(
                                new LambdaQueryWrapper<DispatchSolveJobDO>()
                                                .eq(DispatchSolveJobDO::getTenantId, tenantId)
                                                .eq(DispatchSolveJobDO::getPlanId, planId)
                                                .eq(DispatchSolveJobDO::getTaskId, taskId)
                                                .eq(DispatchSolveJobDO::getDeleted, 0));

                if (job == null) {
                        return CommonResult.error(404, "Solve task not found");
                }

                return CommonResult.success(
                                new SolveTaskStatusVO(job.getTaskId(), job.getStatus(), job.getMessage()));
        }
}
