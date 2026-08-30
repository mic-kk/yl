package com.tcm.integration.pay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 银联支付适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class PayAdapterMockImpl implements PayAdapter {

    @Override
    public String createPayment(Map<String, Object> paymentReq) {
        log.info("[Pay-Mock] 创建支付: {}", paymentReq);
        return "MOCK-PAY-" + System.currentTimeMillis();
    }

    @Override
    public void handlePayCallback(Map<String, Object> callbackBody) {
        log.info("[Pay-Mock] 接收支付回调: {}", callbackBody);
        // 实际实现：按回调幂等键（支付单号+金额）校验后更新支付状态，后续迭代完成
    }

    @Override
    public void refund(String paymentNo, Long amount) {
        log.info("[Pay-Mock] 退款: paymentNo={}, amount={}", paymentNo, amount);
    }
}
