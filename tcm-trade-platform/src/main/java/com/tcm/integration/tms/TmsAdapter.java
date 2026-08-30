package com.tcm.integration.tms;

import java.util.Map;

/**
 * TMS 物流适配器：发货指令下发、物流轨迹回传。
 * 约束：只做报文转换/协议适配，不写业务逻辑；外部回调必须幂等。
 */
public interface TmsAdapter {

    /** 下发发货指令 */
    void pushShipment(Long orderId, Map<String, Object> shipmentInfo);

    /** 接收物流轨迹回调（实现内必须做幂等校验） */
    void handleTraceCallback(Map<String, Object> callbackBody);
}
