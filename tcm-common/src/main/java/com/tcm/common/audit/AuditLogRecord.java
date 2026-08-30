package com.tcm.common.audit;

import lombok.Builder;
import lombok.Getter;

/**
 * 审计记录体：由切面构建，异步写入 tcm_audit_log。
 */
@Getter
@Builder
public class AuditLogRecord {

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
    private long costTime;
    private boolean success;

    /**
     * 快照提供者：方法首个参数实现此接口时，切面记录修改前后快照。
     * 如 DTO 携带旧值/新值，实现两个方法返回 JSON 摘要即可。
     */
    public interface SnapshotProvider {
        /** 执行前的状态摘要 */
        String provideBeforeSnapshot();

        /** 执行后的状态摘要 */
        String provideAfterSnapshot();
    }
}
