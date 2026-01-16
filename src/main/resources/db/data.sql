-- =========================================================
-- 目标：
-- 1) 严格匹配：tenant_id/creator/create_time/updater/update_time/deleted(TINYINT)
-- 2) 可跑通一次 Solver：CREATED → RUNNING → SOLVED
-- 3) 有 routes + unassigned（通过插入一个超载任务触发丢弃/未分配）
-- =========================================================
SET
    NAMES utf8mb4;

SET
    FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- 0) 清理历史数据（按依赖顺序）
-- ---------------------------------------------------------
DELETE FROM
    dispatch_route_stop
WHERE
    tenant_id = 1
    AND plan_id = 1;

DELETE FROM
    dispatch_route
WHERE
    tenant_id = 1
    AND plan_id = 1;

DELETE FROM
    dispatch_unassigned
WHERE
    tenant_id = 1
    AND plan_id = 1;

DELETE FROM
    dispatch_task
WHERE
    tenant_id = 1
    AND plan_id = 1;

DELETE FROM
    dispatch_plan
WHERE
    tenant_id = 1
    AND id = 1;

DELETE FROM
    dispatch_vehicle
WHERE
    tenant_id = 1
    AND id IN (1, 2);

-- ---------------------------------------------------------
-- 1) 插入方案：dispatch_plan
-- ---------------------------------------------------------
INSERT INTO
    dispatch_plan (
        id,
        tenant_id,
        plan_code,
        status,
        message,
        time_limit_sec,
        unassigned_penalty,
        allow_drop,
        total_distance_m,
        total_time_sec,
        assigned_count,
        unassigned_count,
        solve_millis,
        creator,
        create_time,
        updater,
        update_time,
        deleted
    )
VALUES
    (
        1,
        1,
        'PLAN_202601_MOCK',
        'CREATED',
        NULL,
        10,
        10000,
        1,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    );

-- ---------------------------------------------------------
-- 2) 插入车辆：dispatch_vehicle
--    两台车，容量 100，全天可用
-- ---------------------------------------------------------
INSERT INTO
    dispatch_vehicle (
        id,
        tenant_id,
        vehicle_code,
        start_node_id,
        end_node_id,
        capacity_weight,
        capacity_volume,
        work_start_sec,
        work_end_sec,
        fixed_cost,
        cost_per_meter,
        status,
        creator,
        create_time,
        updater,
        update_time,
        deleted
    )
VALUES
    (
        1,
        1,
        'VH-001',
        100,
        100,
        100,
        NULL,
        0,
        86400,
        NULL,
        NULL,
        'AVAILABLE',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        2,
        1,
        'VH-002',
        101,
        101,
        100,
        NULL,
        0,
        86400,
        NULL,
        NULL,
        'AVAILABLE',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    );

-- ---------------------------------------------------------
-- 3) 插入任务：dispatch_task
--    6 个正常任务（每个 demand_weight=10）必然可分配
--    1 个超载任务（demand_weight=1000）必然无法分配 -> unassigned
-- ---------------------------------------------------------
INSERT INTO
    dispatch_task (
        id,
        tenant_id,
        plan_id,
        task_code,
        node_id,
        lat,
        lng,
        tw_start_sec,
        tw_end_sec,
        service_time_sec,
        demand_weight,
        demand_volume,
        priority,
        status,
        creator,
        create_time,
        updater,
        update_time,
        deleted
    )
VALUES
    (
        1,
        1,
        1,
        'TASK-001',
        201,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        2,
        1,
        1,
        'TASK-002',
        202,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        3,
        1,
        1,
        'TASK-003',
        203,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        4,
        1,
        1,
        'TASK-004',
        204,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        5,
        1,
        1,
        'TASK-005',
        205,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    (
        6,
        1,
        1,
        'TASK-006',
        206,
        NULL,
        NULL,
        0,
        86400,
        300,
        10,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    ),
    -- 超载任务（必然未分配）
    (
        7,
        1,
        1,
        'TASK-OVERLOAD',
        207,
        NULL,
        NULL,
        0,
        86400,
        300,
        1000,
        NULL,
        1,
        'WAITING',
        'admin',
        NOW(),
        'admin',
        NOW(),
        0
    );

SET
    FOREIGN_KEY_CHECKS = 1;