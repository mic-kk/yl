package com.tcm.integration.tms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TMS 适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class TmsAdapterMockImpl implements TmsAdapter {

    @Override
    public void pushShipment(Long orderId, Map<String, Object> shipmentInfo) {
        log.info("[TMS-Mock] 下发发货指令: orderId={}, info={}", orderId, shipmentInfo);
    }

    @Override
    public void handleTraceCallback(Map<String, Object> callbackBody) {
        log.info("[TMS-Mock] 接收物流轨迹回调: {}", callbackBody);
    }
}
