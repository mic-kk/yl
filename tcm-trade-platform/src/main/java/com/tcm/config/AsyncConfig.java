package com.tcm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置：审计日志使用独立线程池（手动创建，禁止 Executors）。
 * 队列满时丢弃并记录错误日志——审计失败绝不阻断业务。
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("audit-thread-");
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor pool) ->
                log.error("审计线程池已满，丢弃审计任务"));
        executor.initialize();
        return executor;
    }
}
