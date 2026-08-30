package com.tcm.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 审计切面：@AuditLog 方法环绕执行，构建记录后异步落库。
 * 审计失败仅记日志，绝不阻断业务。
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogWriter auditLogWriter;
    private final List<OperatorProvider> operatorProviders;

    public AuditLogAspect(AuditLogWriter auditLogWriter,
                          @Autowired(required = false) List<OperatorProvider> operatorProviders) {
        this.auditLogWriter = auditLogWriter;
        this.operatorProviders = operatorProviders;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            try {
                long cost = System.currentTimeMillis() - start;
                AuditLogRecord record = AuditLogRecordFactory.build(auditLog, joinPoint.getArgs(),
                        resolveOperatorId(), resolveOperatorName(), resolveRequestIp(),
                        result, cost, success);
                auditLogWriter.write(record);
            } catch (Exception e) {
                log.error("审计日志写入失败（不阻断业务）: module={}, action={}", auditLog.module(), auditLog.action(), e);
            }
        }
    }

    private Long resolveOperatorId() {
        if (operatorProviders != null && !operatorProviders.isEmpty()) {
            return operatorProviders.get(0).currentOperatorId();
        }
        return null;
    }

    private String resolveOperatorName() {
        if (operatorProviders != null && !operatorProviders.isEmpty()) {
            return operatorProviders.get(0).currentOperatorName();
        }
        return null;
    }

    private String resolveRequestIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
