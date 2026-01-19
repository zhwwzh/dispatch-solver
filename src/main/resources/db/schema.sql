-- =========================================================
-- 建表语句
-- 规范：
-- 1) 字段命名：create_time / update_time；creator / updater
-- 2) 逻辑删除：deleted（0=未删除，1=已删除）
-- 3) 多租户：tenant_id
-- 4) 字符集：utf8mb4
-- =========================================================
SET
    NAMES utf8mb4;

SET
    FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1) 调度方案表：dispatch_plan
-- =========================================================
DROP TABLE IF EXISTS dispatch_plan;

CREATE TABLE dispatch_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    plan_code VARCHAR(64) NOT NULL COMMENT '方案编码',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/RUNNING/SOLVED/FAILED',
    message VARCHAR(255) NULL COMMENT '状态说明/失败原因',
    time_limit_sec INT NULL COMMENT '求解时限（秒）',
    unassigned_penalty BIGINT NULL COMMENT '未分配惩罚（允许丢弃时）',
    allow_drop TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许丢弃：1=是，0=否',
    total_distance_m BIGINT NULL COMMENT '总距离（米）',
    total_time_sec BIGINT NULL COMMENT '总时间（秒）',
    assigned_count INT NULL COMMENT '已分配任务数',
    unassigned_count INT NULL COMMENT '未分配任务数',
    solve_millis BIGINT NULL COMMENT '求解耗时（毫秒）',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plan_code (tenant_id, plan_code),
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_tenant_update_time (tenant_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度方案';

-- =========================================================
-- 2) 车辆资源表：dispatch_vehicle
-- =========================================================
DROP TABLE IF EXISTS dispatch_vehicle;

CREATE TABLE dispatch_vehicle (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    vehicle_code VARCHAR(64) NOT NULL COMMENT '车辆编码（车牌/内部编码）',
    start_node_id BIGINT UNSIGNED NOT NULL COMMENT '起点节点ID（车场/网点）',
    end_node_id BIGINT UNSIGNED NOT NULL COMMENT '终点节点ID（车场/网点）',
    capacity_weight INT NULL COMMENT '载重容量（单位自定）',
    capacity_volume INT NULL COMMENT '体积容量（单位自定）',
    work_start_sec INT NULL COMMENT '上班时间（秒，0~86400）',
    work_end_sec INT NULL COMMENT '下班时间（秒，0~86400）',
    fixed_cost BIGINT NULL COMMENT '固定成本（可选）',
    cost_per_meter DOUBLE NULL COMMENT '单位距离成本（可选）',
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/...',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_vehicle_code (tenant_id, vehicle_code),
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_tenant_nodes (tenant_id, start_node_id, end_node_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车辆资源';

-- =========================================================
-- 3) 调度任务表：dispatch_task
-- =========================================================
DROP TABLE IF EXISTS dispatch_task;

CREATE TABLE dispatch_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '方案ID（隔离不同求解批次）',
    task_code VARCHAR(64) NOT NULL COMMENT '任务编码（运单号/包裹号）',
    node_id BIGINT UNSIGNED NOT NULL COMMENT '任务节点ID（客户/网点/站点）',
    lat DOUBLE NULL COMMENT '纬度（可选）',
    lng DOUBLE NULL COMMENT '经度（可选）',
    tw_start_sec INT NULL COMMENT '时间窗开始（秒，0~86400）',
    tw_end_sec INT NULL COMMENT '时间窗结束（秒，0~86400）',
    service_time_sec INT NULL COMMENT '服务时长（秒）',
    demand_weight INT NULL COMMENT '需求载重（单位自定）',
    demand_volume INT NULL COMMENT '需求体积（单位自定）',
    priority INT NULL COMMENT '优先级（数值越大越优先）',
    status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '状态：WAITING/...',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plan_task_code (tenant_id, plan_id, task_code),
    KEY idx_tenant_plan_status (tenant_id, plan_id, status),
    KEY idx_tenant_plan_node (tenant_id, plan_id, node_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度任务';

-- =========================================================
-- 4) 调度路线表：dispatch_route
-- =========================================================
DROP TABLE IF EXISTS dispatch_route;

CREATE TABLE dispatch_route (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '方案ID',
    vehicle_id BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
    driver_id BIGINT UNSIGNED NULL COMMENT '司机ID（可选）',
    total_distance_m BIGINT NULL COMMENT '该路线总距离（米）',
    total_time_sec BIGINT NULL COMMENT '该路线总时间（秒）',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_tenant_plan (tenant_id, plan_id),
    KEY idx_tenant_plan_vehicle (tenant_id, plan_id, vehicle_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度路线';

-- =========================================================
-- 5) 路线停靠点表：dispatch_route_stop
-- =========================================================
DROP TABLE IF EXISTS dispatch_route_stop;

CREATE TABLE dispatch_route_stop (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '方案ID',
    route_id BIGINT UNSIGNED NOT NULL COMMENT '路线ID',
    seq INT NOT NULL COMMENT '停靠顺序（从0开始）',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
    node_id BIGINT UNSIGNED NOT NULL COMMENT '节点ID',
    eta_sec BIGINT NULL COMMENT '预计到达（秒）',
    etd_sec BIGINT NULL COMMENT '预计离开（秒）',
    service_time_sec INT NULL COMMENT '服务时长（秒）',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_tenant_plan_route (tenant_id, plan_id, route_id),
    KEY idx_tenant_plan_task (tenant_id, plan_id, task_id),
    KEY idx_route_seq (route_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '路线停靠点';

-- =========================================================
-- 6) 未分配任务表：dispatch_unassigned
-- =========================================================
DROP TABLE IF EXISTS dispatch_unassigned;

CREATE TABLE dispatch_unassigned (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户编号',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '方案ID',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
    reason_code VARCHAR(64) NULL COMMENT '原因码：NO_SOLUTION/DROPPED/...',
    detail VARCHAR(255) NULL COMMENT '原因详情',
    creator VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_tenant_plan (tenant_id, plan_id),
    KEY idx_tenant_plan_task (tenant_id, plan_id, task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '未分配任务';

-- =========================================================
-- 7) 调度求解异步任务表：dispatch_solve_job
-- =========================================================
DROP TABLE IF EXISTS dispatch_solve_job;

CREATE TABLE dispatch_solve_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    plan_id BIGINT NOT NULL COMMENT '调度方案ID',
    task_id VARCHAR(64) NOT NULL COMMENT '求解任务ID（对外暴露，用于查询任务状态）',
    status VARCHAR(32) NOT NULL COMMENT '任务状态：ACCEPTED / RUNNING / SOLVED / FAILED',
    message VARCHAR(255) DEFAULT NULL COMMENT '状态说明或失败原因',
    create_time DATETIME NOT NULL COMMENT '任务创建时间',
    update_time DATETIME NOT NULL COMMENT '任务状态更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_tenant_plan (tenant_id, plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度求解异步任务表';

CREATE INDEX idx_tenant_plan_status_update ON dispatch_solve_job(tenant_id, plan_id, status, deleted, update_time);

SET
    FOREIGN_KEY_CHECKS = 1;