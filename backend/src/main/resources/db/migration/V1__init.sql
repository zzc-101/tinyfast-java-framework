-- =============================================
-- Base Framework 数据库初始化脚本（Flyway V1）
-- 数据库需提前创建，应用启动时自动执行
-- =============================================

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码',
    `nickname`    VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    `email`       VARCHAR(128) DEFAULT '' COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    `avatar`      VARCHAR(256) DEFAULT '' COMMENT '头像地址',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态(1:正常 0:禁用)',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `role_key`    VARCHAR(64)  NOT NULL COMMENT '角色标识',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态(1:正常 0:禁用)',
    `remark`      VARCHAR(256) DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB COMMENT='角色表';

-- ----------------------------
-- 用户角色关联表
-- ----------------------------
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- ----------------------------
-- 菜单/权限表
-- ----------------------------
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父菜单ID(0为顶级)',
    `menu_name`   VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `menu_type`   CHAR(1)      NOT NULL COMMENT '类型(D:目录 M:菜单 B:按钮)',
    `path`        VARCHAR(256) DEFAULT '' COMMENT '路由地址',
    `component`   VARCHAR(256) DEFAULT '' COMMENT '组件路径',
    `permission`  VARCHAR(128) DEFAULT '' COMMENT '权限标识',
    `icon`        VARCHAR(64)  DEFAULT '' COMMENT '菜单图标',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `visible`     TINYINT      DEFAULT 1 COMMENT '是否可见(1:是 0:否)',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态(1:正常 0:禁用)',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- ----------------------------
-- 角色菜单关联表
-- ----------------------------
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

-- ----------------------------
-- 操作日志表
-- ----------------------------
CREATE TABLE `sys_operation_log` (
    `id`             BIGINT        NOT NULL COMMENT '主键ID',
    `module`         VARCHAR(64)   DEFAULT '' COMMENT '操作模块',
    `description`    VARCHAR(256)  DEFAULT '' COMMENT '操作描述',
    `request_method` VARCHAR(10)   DEFAULT '' COMMENT '请求方式',
    `request_url`    VARCHAR(256)  DEFAULT '' COMMENT '请求URL',
    `request_params` TEXT          COMMENT '请求参数',
    `response_body`  TEXT          COMMENT '响应结果',
    `status`         TINYINT       DEFAULT 1 COMMENT '操作状态(1:成功 0:失败)',
    `error_msg`      TEXT          COMMENT '错误信息',
    `cost_time`      BIGINT        DEFAULT 0 COMMENT '耗时(毫秒)',
    `operator_id`    BIGINT        DEFAULT NULL COMMENT '操作人ID',
    `operator_name`  VARCHAR(64)   DEFAULT '' COMMENT '操作人名称',
    `operator_ip`    VARCHAR(64)   DEFAULT '' COMMENT '操作人IP',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ----------------------------
-- 登录日志表
-- ----------------------------
CREATE TABLE `sys_login_log` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `username`    VARCHAR(64)  DEFAULT '' COMMENT '登录用户名',
    `login_ip`    VARCHAR(64)  DEFAULT '' COMMENT '登录IP',
    `login_location` VARCHAR(128) DEFAULT '' COMMENT '登录地点',
    `browser`     VARCHAR(64)  DEFAULT '' COMMENT '浏览器',
    `os`          VARCHAR(64)  DEFAULT '' COMMENT '操作系统',
    `status`      TINYINT      DEFAULT 1 COMMENT '登录状态(1:成功 0:失败)',
    `message`     VARCHAR(256) DEFAULT '' COMMENT '提示消息',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='登录日志表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
-- 默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);

-- 默认角色
INSERT INTO `sys_role` (`id`, `role_key`, `role_name`, `sort`, `status`) VALUES
(1, 'ADMIN', '管理员', 1, 1),
(2, 'USER', '普通用户', 2, 1);

-- 管理员角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ----------------------------
-- 菜单数据
-- 类型 D:目录 M:菜单 B:按钮；权限标识格式 模块:资源:操作
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`) VALUES
-- 仪表盘
(1,    0,  '仪表盘',   'M', '/dashboard',       'dashboard/index',       '',                          'DashboardOutlined', 1),
-- 系统管理
(2,    0,  '系统管理', 'D', '/system',          '',                      '',                          'SettingOutlined',   2),
(21,   2,  '用户管理', 'M', '/system/user',     'system/UserList',       'system:user:list',          'UserOutlined',      1),
(211,  21, '用户新增', 'B', '',                 '',                      'system:user:add',           '',                  1),
(212,  21, '用户修改', 'B', '',                 '',                      'system:user:edit',          '',                  2),
(213,  21, '用户删除', 'B', '',                 '',                      'system:user:delete',        '',                  3),
(214,  21, '重置密码', 'B', '',                 '',                      'system:user:resetPwd',      '',                  4),
(22,   2,  '角色管理', 'M', '/system/role',     'system/RoleList',       'system:role:list',          'TeamOutlined',      2),
(221,  22, '角色新增', 'B', '',                 '',                      'system:role:add',           '',                  1),
(222,  22, '角色修改', 'B', '',                 '',                      'system:role:edit',          '',                  2),
(223,  22, '角色删除', 'B', '',                 '',                      'system:role:delete',        '',                  3),
(23,   2,  '菜单管理', 'M', '/system/menu',     'system/MenuList',       'system:menu:list',          'MenuOutlined',      3),
(231,  23, '菜单新增', 'B', '',                 '',                      'system:menu:add',           '',                  1),
(232,  23, '菜单修改', 'B', '',                 '',                      'system:menu:edit',          '',                  2),
(233,  23, '菜单删除', 'B', '',                 '',                      'system:menu:delete',        '',                  3),
-- 系统监控
(3,    0,  '系统监控', 'D', '/monitor',         '',                      '',                          'MonitorOutlined',   3),
(31,   3,  '服务监控', 'M', '/monitor/server',  'monitor/ServerInfo',    'monitor:server:list',       '',                  1),
(32,   3,  '在线用户', 'M', '/monitor/online',  'monitor/OnlineUser',    'monitor:online:list',       '',                  2),
(321,  32, '强退用户', 'B', '',                 '',                      'monitor:online:forceLogout','',                  1),
-- 日志管理
(4,    0,  '日志管理', 'D', '/log',             '',                      '',                          'FileTextOutlined',  4),
(41,   4,  '操作日志', 'M', '/log/operation',   'log/OperationLog',      'log:operation:list',        '',                  1),
(411,  41, '清空操作日志', 'B', '',             '',                      'log:operation:clear',       '',                  1),
(42,   4,  '登录日志', 'M', '/log/login',       'log/LoginLog',          'log:login:list',            '',                  2),
(421,  42, '清空登录日志', 'B', '',             '',                      'log:login:clear',           '',                  1);

-- ADMIN 角色关联全部菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 1, `id` FROM `sys_menu`;

-- USER 角色只有仪表盘
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1);
