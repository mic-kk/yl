package com.tcm.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * MQ 消费者基类。
 * 子类实现 dedupKey（业务唯一 ID）与 doConsume（抛异常表示消费失败）。
 * 幂等：Redis SETNX 去重；业务失败时删除幂等标记，交给 MQ 重试；超过重试上限自动进入 %DLQ% 死信队列。
 */
@Slf4j
public abstract class AbstractMqConsumer {

    private static final String DEDUP_KEY_PREFIX = "tcm:mq:dedup:";

    private final StringRedisTemplate stringRedisTemplate;

    protected AbstractMqConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 业务唯一 ID（如订单号、工单号），同一消息幂等去重依据。 */
    protected abstract String dedupKey(Object payload);

    /** 消费逻辑，抛异常表示消费失败（触发 MQ 重试）。 */
    protected abstract void doConsume(Object payload);

    /**
     * 消费入口：幂等拦截 + 业务处理。
     *
     * @return true=消费成功或重复消息；false=业务失败（等待 MQ 重试）
     */
    protected boolean tryConsume(Object payload, long dedupSeconds) {
        String dedupKey = DEDUP_KEY_PREFIX + dedupKey(payload);
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(dedupSeconds));
        if (Boolean.FALSE.equals(first)) {
            log.info("幂等拦截重复消息: {}", dedupKey);
            return true;
        }
        try {
            doConsume(payload);
            return true;
        } catch (Exception e) {
            log.error("消息消费失败，删除幂等标记等待重试: {}", dedupKey, e);
            stringRedisTemplate.delete(dedupKey);
            return false;
        }
    }
}
