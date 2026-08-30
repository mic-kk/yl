package com.tcm.integration.regulatory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监管适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class RegulatoryAdapterMockImpl implements RegulatoryAdapter {

    @Override
    public void reportTaxData(Map<String, Object> taxData) {
        log.info("[Regulatory-Mock] 上报税务数据: {}", taxData);
    }

    @Override
    public void reportSupervisionData(Map<String, Object> data) {
        log.info("[Regulatory-Mock] 上报药监数据: {}", data);
    }
}
