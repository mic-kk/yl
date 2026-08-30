package com.tcm.integration.wms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WMS 适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class WmsAdapterMockImpl implements WmsAdapter {

    @Override
    public void pushOutboundOrder(Long orderId, Map<String, Object> outboundInfo) {
        log.info("[WMS-Mock] 下发出库指令: orderId={}, info={}", orderId, outboundInfo);
    }

    @Override
    public void handleOutboundCallback(Map<String, Object> callbackBody) {
        log.info("[WMS-Mock] 接收出库回调: {}", callbackBody);
        // 实际实现：按回调幂等键（如出库单号）校验后更新订单状态，后续迭代完成
    }
}
