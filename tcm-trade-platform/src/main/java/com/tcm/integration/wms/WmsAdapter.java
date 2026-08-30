package com.tcm.integration.wms;

import java.util.Map;

/**
 * WMS 仓储适配器：订单出库指令下发、出库结果回调。
 * 约束：只做报文转换/协议适配，不写业务逻辑；外部回调必须幂等。
 */
public interface WmsAdapter {

    /** 下发订单出库指令 */
    void pushOutboundOrder(Long orderId, Map<String, Object> outboundInfo);

    /** 接收 WMS 出库回调（实现内必须做幂等校验） */
    void handleOutboundCallback(Map<String, Object> callbackBody);
}
