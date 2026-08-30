package com.tcm.config;

import cn.dev33.satoken.stp.StpUtil;
import com.tcm.common.audit.OperatorProvider;
import org.springframework.stereotype.Component;

/**
 * 审计操作人提供者：基于 Sa-Token 会话。
 */
@Component
public class SaOperatorProvider implements OperatorProvider {

    @Override
    public Long currentOperatorId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String currentOperatorName() {
        try {
            return String.valueOf(StpUtil.getLoginIdDefaultNull());
        } catch (Exception e) {
            return null;
        }
    }
}
