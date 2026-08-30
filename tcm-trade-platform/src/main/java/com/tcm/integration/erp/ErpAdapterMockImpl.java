package com.tcm.integration.erp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ERP 适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class ErpAdapterMockImpl implements ErpAdapter {

    @Override
    public void syncProduct(Map<String, Object> productData) {
        log.info("[ERP-Mock] 同步商品资料: {}", productData);
    }

    @Override
    public void syncInventory(Map<String, Object> inventoryData) {
        log.info("[ERP-Mock] 同步库存数据: {}", inventoryData);
    }

    @Override
    public void pushOrderToErp(Long orderId, Map<String, Object> orderData) {
        log.info("[ERP-Mock] 下发订单至商家 ERP: orderId={}, data={}", orderId, orderData);
    }
}
