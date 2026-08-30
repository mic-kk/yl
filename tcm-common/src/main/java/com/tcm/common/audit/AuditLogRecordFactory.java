package com.tcm.common.audit;

import cn.hutool.json.JSONUtil;
import com.tcm.common.utils.SensitiveDataUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 审计记录构建工厂：纯函数式，不依赖 Spring，便于单元测试。
 */
public final class AuditLogRecordFactory {

    private static final int MAX_SNAPSHOT_LENGTH = 2048;

    private AuditLogRecordFactory() {
    }

    public static AuditLogRecord build(AuditLog annotation, Object[] args, Long operatorId,
                                       String operatorName, String requestIp,
                                       Object result, long costTime, boolean success) {
        String params = serializeArgs(args);
        String before = null;
        String after = null;
        if (args != null && args.length > 0 && args[0] instanceof AuditLogRecord.SnapshotProvider provider) {
            before = truncate(provider.provideBeforeSnapshot());
            after = truncate(provider.provideAfterSnapshot());
        }
        String response = annotation.logResponse() ? truncate(toJson(result)) : null;
        return AuditLogRecord.builder()
                .module(annotation.module())
                .action(annotation.action())
                .description(annotation.description())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .requestIp(requestIp)
                .paramsSnapshot(truncate(params))
                .beforeSnapshot(before)
                .afterSnapshot(after)
                .responseSnapshot(response)
                .costTime(costTime)
                .success(success)
                .build();
    }

    /** 序列化方法参数：跳过 Servlet/文件等不可序列化对象，脱敏敏感字段。 */
    private static String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof MultipartFile || arg instanceof jakarta.servlet.ServletRequest
                    || arg instanceof jakarta.servlet.ServletResponse
                    || arg instanceof jakarta.servlet.http.HttpSession) {
                continue;
            }
            list.add(normalizeArg(arg));
        }
        if (list.isEmpty()) {
            return null;
        }
        return SensitiveDataUtil.maskJson(toJson(list));
    }

    /** 字符串参数若本身是 JSON（如消费者场景的报文），解析为对象以便递归脱敏。 */
    private static Object normalizeArg(Object arg) {
        if (arg instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    return JSONUtil.parse(trimmed);
                } catch (Exception ignored) {
                    // 非合法 JSON，按普通字符串处理
                }
            }
        }
        return arg;
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_SNAPSHOT_LENGTH ? text : text.substring(0, MAX_SNAPSHOT_LENGTH);
    }
}
