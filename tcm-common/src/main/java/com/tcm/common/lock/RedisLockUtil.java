package com.tcm.common.lock;

import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的分布式锁工具。
 * 约定：锁粒度必须小，锁业务唯一标识（如订单号/批次号），禁止锁表锁模块。
 */
@Slf4j
public class RedisLockUtil {

    private static final String LOCK_KEY_PREFIX = "tcm:lock:";

    private final RedissonClient redissonClient;

    public RedisLockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private RLock getLock(String key) {
        return redissonClient.getLock(LOCK_KEY_PREFIX + key);
    }

    /** 尝试加锁，waitMillis 内拿不到返回 false。 */
    public boolean tryLock(String key, long waitMillis, long leaseMillis) {
        RLock lock = getLock(key);
        try {
            return lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 解锁；非持锁线程调用仅记日志不抛异常。 */
    public void unlock(String key) {
        RLock lock = getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        } else {
            log.warn("释放锁失败：当前线程未持有该锁 key={}", key);
        }
    }

    /** 回调式加锁执行：加锁失败抛业务异常。 */
    public <T> T executeWithLock(String key, long waitMillis, long leaseMillis, Supplier<T> action) {
        if (!tryLock(key, waitMillis, leaseMillis)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分布式锁失败: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }

    /** 回调式加锁执行（无返回值）。 */
    public void executeWithLock(String key, long waitMillis, long leaseMillis, Runnable action) {
        executeWithLock(key, waitMillis, leaseMillis, () -> {
            action.run();
            return null;
        });
    }
}
