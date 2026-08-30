package com.tcm.common.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redis 分布式锁配置：tcm.infrastructure.redis.enabled=true 时启用。
 * 默认关闭，避免本地未装 Redis 时启动失败。手动创建 RedissonClient，不依赖 starter 自动装配。
 */
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "redis.enabled", havingValue = "true")
public class RedisLockConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    public RedisLockUtil redisLockUtil(RedissonClient redissonClient) {
        return new RedisLockUtil(redissonClient);
    }
}
