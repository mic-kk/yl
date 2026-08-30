package com.tcm.common.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tcm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志表：全平台核心操作留痕，禁止物理删除。
 */
@Getter
@Setter
@TableName("tcm_audit_log")
public class AuditLogEntity extends BaseEntity {

    private String module;
    private String action;
    private String description;
    private Long operatorId;
    private String operatorName;
    private String requestIp;
    private String paramsSnapshot;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String responseSnapshot;
    private Long costTime;
    private Boolean success;
}
