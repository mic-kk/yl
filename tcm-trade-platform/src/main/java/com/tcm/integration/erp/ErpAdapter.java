package com.tcm.integration.erp;

import java.util.Map;

/**
 * 商家 ERP 适配器：商品/库存数据双向同步、订单下发。
 * 约束：只做报文转换/协议适配，不写业务逻辑。
 */
public interface ErpAdapter {

    /** 同步商品资料（供应商/卖家 ERP → 平台） */
    void syncProduct(Map<String, Object> productData);

    /** 同步库存数据（卖家自有仓 ERP → 平台） */
    void syncInventory(Map<String, Object> inventoryData);

    /** 订单下发至商家 ERP（卖家自有仓履约） */
    void pushOrderToErp(Long orderId, Map<String, Object> orderData);
}
