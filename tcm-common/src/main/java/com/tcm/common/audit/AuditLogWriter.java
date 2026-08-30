package com.tcm.common.audit;

import com.tcm.common.audit.entity.AuditLogEntity;
import com.tcm.common.audit.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步写入器：独立线程池执行，失败仅记日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogMapper auditLogMapper;

    @Async("auditExecutor")
    public void write(AuditLogRecord record) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setModule(record.getModule());
            entity.setAction(record.getAction());
            entity.setDescription(record.getDescription());
            entity.setOperatorId(record.getOperatorId());
            entity.setOperatorName(record.getOperatorName());
            entity.setRequestIp(record.getRequestIp());
            entity.setParamsSnapshot(record.getParamsSnapshot());
            entity.setBeforeSnapshot(record.getBeforeSnapshot());
            entity.setAfterSnapshot(record.getAfterSnapshot());
            entity.setResponseSnapshot(record.getResponseSnapshot());
            entity.setCostTime(record.getCostTime());
            entity.setSuccess(record.isSuccess());
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("审计日志落库失败（不阻断业务）: {}", record, e);
        }
    }
}
