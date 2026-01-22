package net.mbi.wcloud.dispatch.solver.service.plan;

import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchPlanDO;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchSolveJobDO;
import net.mbi.wcloud.dispatch.solver.dal.mysql.*;
import net.mbi.wcloud.dispatch.solver.framework.lock.DistributedLock;
import net.mbi.wcloud.dispatch.solver.ortools.OrToolsSolverEngine;
import net.mbi.wcloud.dispatch.solver.service.plan.dto.SolveRequestDTO;
import net.mbi.wcloud.dispatch.solver.service.plan.model.SolveTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 单测目标：钉死 submitSolve 的幂等与并发锁逻辑
 *
 * 覆盖场景：
 * 1) 直接幂等命中：findActiveJob != null -> 直接返回 taskId
 * 2) tryLock 失败后再次查询幂等命中 -> 返回 taskId
 * 3) 正常创建：无活跃任务 + tryLock 成功 -> insert job + markStatus(ACCEPTED) + runAsync
 * 被触发（但这里 stub 掉）
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PlanSolveServiceImplTest {

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private DispatchPlanMapper planMapper;
    @Mock
    private DispatchTaskMapper taskMapper;
    @Mock
    private DispatchVehicleMapper vehicleMapper;
    @Mock
    private DispatchRouteMapper routeMapper;
    @Mock
    private DispatchRouteStopMapper stopMapper;
    @Mock
    private DispatchUnassignedMapper unassignedMapper;
    @Mock
    private DispatchSolveJobMapper solveJobMapper;
    @Mock
    private OrToolsSolverEngine solverEngine;

    /** 用 spy 来 stub runAsync，避免单测跑进 solver/组装逻辑 */
    private PlanSolveServiceImpl serviceSpy;

    @BeforeEach
    void setUp() {
        PlanSolveServiceImpl real = new PlanSolveServiceImpl(
                distributedLock,
                planMapper,
                taskMapper,
                vehicleMapper,
                routeMapper,
                stopMapper,
                unassignedMapper,
                solveJobMapper,
                solverEngine);
        serviceSpy = spy(real);

        // 关键：stub 掉 runAsync，避免执行 markStatus(RUNNING)+solverEngine.solve+unlock 等
        doNothing().when(serviceSpy).runAsync(any(SolveRequestDTO.class), anyString());
    }

    @Test
    void submitSolve_idempotentHit_shouldReturnSameTaskId_andNotLockAndNotInsert() {
        // given
        long tenantId = 1L;
        long planId = 1001L;
        String existTaskId = "solve-1-1001-1234567890";

        SolveRequestDTO req = buildReq(tenantId, planId);

        DispatchSolveJobDO active = new DispatchSolveJobDO();
        active.setTenantId(tenantId);
        active.setPlanId(planId);
        active.setTaskId(existTaskId);
        active.setStatus(SolveTaskStatus.RUNNING.code());
        active.setDeleted(0);
        active.setUpdateTime(LocalDateTime.now());

        when(solveJobMapper.selectOne(any())).thenReturn(active);

        // when
        String taskId = serviceSpy.submitSolve(req);

        // then
        assertEquals(existTaskId, taskId);

        verify(distributedLock, never()).tryLock(anyString(), anyInt());
        verify(solveJobMapper, never()).insert(any(DispatchSolveJobDO.class));
        verify(planMapper, never()).updateById(any(DispatchPlanDO.class));
        verify(serviceSpy, never()).runAsync(any(SolveRequestDTO.class), anyString());
    }

    @Test
    void submitSolve_lockBusy_thenSecondQueryHit_shouldReturnSameTaskId_andNotInsert() {
        // given
        long tenantId = 1L;
        long planId = 1001L;
        String existTaskId = "solve-1-1001-2222222222";
        SolveRequestDTO req = buildReq(tenantId, planId);

        DispatchSolveJobDO active = new DispatchSolveJobDO();
        active.setTenantId(tenantId);
        active.setPlanId(planId);
        active.setTaskId(existTaskId);
        active.setStatus(SolveTaskStatus.ACCEPTED.code());
        active.setDeleted(0);
        active.setUpdateTime(LocalDateTime.now());

        when(solveJobMapper.selectOne(any()))
                .thenReturn(null) // 第一次 findActiveJob
                .thenReturn(active); // tryLock 失败后第二次 findActiveJob

        when(distributedLock.tryLock(eq("solve:" + tenantId + ":" + planId), eq(60)))
                .thenReturn(false);

        // when
        String taskId = serviceSpy.submitSolve(req);

        // then
        assertEquals(existTaskId, taskId);

        verify(distributedLock, times(1)).tryLock(eq("solve:" + tenantId + ":" + planId), eq(60));

        verify(solveJobMapper, never()).insert(any(DispatchSolveJobDO.class));
        verify(planMapper, never()).updateById(any(DispatchPlanDO.class));
        verify(serviceSpy, never()).runAsync(any(SolveRequestDTO.class), anyString());
    }

    @Test
    void submitSolve_noActive_andLockSuccess_shouldCreateJob_markAccepted_andTriggerRunAsync() {
        // given
        long tenantId = 1L;
        long planId = 1001L;
        SolveRequestDTO req = buildReq(tenantId, planId);

        when(solveJobMapper.selectOne(any())).thenReturn(null);

        when(distributedLock.tryLock(eq("solve:" + tenantId + ":" + planId), eq(60)))
                .thenReturn(true);

        DispatchPlanDO plan = new DispatchPlanDO();
        plan.setId(planId);
        plan.setTenantId(tenantId);
        plan.setDeleted(0);

        when(planMapper.selectOne(any())).thenReturn(plan);
        when(planMapper.updateById(any(DispatchPlanDO.class))).thenReturn(1);

        when(solveJobMapper.insert(any(DispatchSolveJobDO.class))).thenReturn(1);

        // when
        String taskId = serviceSpy.submitSolve(req);

        // then
        assertNotNull(taskId);
        assertTrue(taskId.startsWith("solve-" + tenantId + "-" + planId + "-"));

        ArgumentCaptor<DispatchSolveJobDO> jobCaptor = ArgumentCaptor.forClass(DispatchSolveJobDO.class);
        verify(solveJobMapper, times(1)).insert(jobCaptor.capture());
        DispatchSolveJobDO inserted = jobCaptor.getValue();
        assertEquals(tenantId, inserted.getTenantId());
        assertEquals(planId, inserted.getPlanId());
        assertEquals(taskId, inserted.getTaskId());
        assertEquals(SolveTaskStatus.ACCEPTED.code(), inserted.getStatus());
        assertEquals(0, inserted.getDeleted());

        ArgumentCaptor<DispatchPlanDO> planCaptor = ArgumentCaptor.forClass(DispatchPlanDO.class);
        verify(planMapper, times(1)).updateById(planCaptor.capture());
        DispatchPlanDO updated = planCaptor.getValue();
        assertEquals(SolveTaskStatus.ACCEPTED.code(), updated.getStatus());
        assertEquals(SolveTaskStatus.ACCEPTED.code(), updated.getMessage());

        verify(serviceSpy, times(1)).runAsync(eq(req), eq(taskId));

        verify(distributedLock, never()).unlock(anyString());
    }

    private SolveRequestDTO buildReq(long tenantId, long planId) {
        SolveRequestDTO req = new SolveRequestDTO();
        req.setTenantId(tenantId);
        req.setPlanId(planId);
        return req;
    }
}
