package com.tcm.integration.pay;

import java.util.Map;

/**
 * 银联支付适配器：支付下单、支付/退款回调。
 * 约束：只做报文转换/协议适配，不写业务逻辑；回调必须幂等。
 */
public interface PayAdapter {

    /** 创建支付（返回支付单号） */
    String createPayment(Map<String, Object> paymentReq);

    /** 接收支付回调（实现内必须做幂等校验） */
    void handlePayCallback(Map<String, Object> callbackBody);

    /** 退款（金额单位：分） */
    void refund(String paymentNo, Long amount);
}
