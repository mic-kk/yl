package com.tcm.common.mq;

import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 统一消息生产者。
 * 约定：所有异步解耦、跨系统通知、耗时操作必须走 MQ，禁止同步调用嵌套。
 * RocketMQTemplate 未配置（rocketmq.name-server 缺失）时降级为告警日志。
 */
@Slf4j
@Component
public class MqProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public MqProducer(@Autowired(required = false) RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /** 发送消息：topic:tag 形式，payload 会被 JSON 序列化。 */
    public void send(String topic, String tag, Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ 未启用（未配置 rocketmq.name-server），消息丢弃: topic={}, tag={}", topic, tag);
            return;
        }
        try {
            rocketMQTemplate.convertAndSend(topic + ":" + tag, payload);
            log.info("MQ 发送成功: topic={}, tag={}", topic, tag);
        } catch (Exception e) {
            log.error("MQ 发送失败: topic={}, tag={}", topic, tag, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息发送失败: " + topic + ":" + tag);
        }
    }
}
