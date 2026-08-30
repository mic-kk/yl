-- ============================================================
-- TCM 平台初始化脚本（达梦 DM8 / MySQL 双兼容）
-- 执行方式：
--   达梦: disql tcm/<密码>@localhost:5236 < init.sql
--   MySQL: mysql -u tcm -p tcm < init.sql
-- 约束：禁止达梦独有语法/函数；分页走 MyBatis-Plus 插件。
-- ============================================================

-- 账号表（6 端共用，角色区分终端）
CREATE TABLE tcm_user (
    id          BIGINT       NOT NULL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
    real_name   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    role_code   VARCHAR(32)  NOT NULL DEFAULT 'buyer_c' COMMENT '角色: admin/seller/supplier/buyer_b/buyer_c/pharmacist',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time TIMESTAMP    DEFAULT NULL,
    update_time TIMESTAMP    DEFAULT NULL,
    create_by   BIGINT       DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除'
);
CREATE UNIQUE INDEX uk_username ON tcm_user (username);
CREATE INDEX idx_user_role ON tcm_user (role_code);

-- 审计日志表（禁止物理删除，保留修改前后快照）
CREATE TABLE tcm_audit_log (
    id                BIGINT       NOT NULL PRIMARY KEY,
    module            VARCHAR(64)  NOT NULL COMMENT '业务模块',
    action            VARCHAR(64)  NOT NULL COMMENT '动作标识',
    description       VARCHAR(255) DEFAULT NULL COMMENT '描述',
    operator_id       BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    request_ip        VARCHAR(64)  DEFAULT NULL COMMENT '请求IP',
    params_snapshot   TEXT         DEFAULT NULL COMMENT '请求参数快照(脱敏)',
    before_snapshot   TEXT         DEFAULT NULL COMMENT '修改前快照',
    after_snapshot    TEXT         DEFAULT NULL COMMENT '修改后快照',
    response_snapshot TEXT         DEFAULT NULL COMMENT '响应摘要',
    cost_time         BIGINT       DEFAULT NULL COMMENT '耗时ms',
    success           TINYINT      DEFAULT 1 COMMENT '是否成功',
    create_time       TIMESTAMP    DEFAULT NULL,
    update_time       TIMESTAMP    DEFAULT NULL,
    create_by         BIGINT       DEFAULT NULL,
    is_deleted        TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_audit_module_time ON tcm_audit_log (module, create_time);

-- 种子账号：admin(平台管理员) / seller01(示例卖家)，密码均为 123456（BCrypt）
INSERT INTO tcm_user (id, username, password, real_name, role_code, status, create_time, update_time, create_by, is_deleted) VALUES
(1830000000000000001, 'admin',    '$2a$10$kcJOox.twy.VFNUVJTzbleJb6Wsdv4j7/LarB1tvyxnt05xV7Xp/S', '平台管理员', 'admin',  1, NOW(), NOW(), 0, 0),
(1830000000000000002, 'seller01', '$2a$10$z6ZVCI9PREd8BoON1fSzG.BB87KwST8OGT.fwqB.VyiUtNNGntFT.', '示例卖家',   'seller', 1, NOW(), NOW(), 0, 0);
