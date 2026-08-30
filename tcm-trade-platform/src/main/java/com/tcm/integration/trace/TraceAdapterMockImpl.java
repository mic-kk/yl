package com.tcm.integration.trace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 溯源适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class TraceAdapterMockImpl implements TraceAdapter {

    @Override
    public void reportTraceNode(Map<String, Object> nodeData) {
        log.info("[Trace-Mock] 上报溯源流转节点: {}", nodeData);
    }

    @Override
    public Map<String, Object> queryTrace(String traceCode) {
        log.info("[Trace-Mock] 查询溯源码: {}", traceCode);
        return new HashMap<>();
    }
}
