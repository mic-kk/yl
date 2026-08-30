package com.tcm.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器：tcm.infrastructure.job.enabled=true 时启用。
 * 约束：禁止使用 Spring Schedule 本地定时任务；所有定时任务统一走 XXL-Job。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses:http://localhost:8080/xxl-job-admin}")
    private String adminAddresses;

    @Value("${xxl.job.access-token:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:tcm-trade-platform}")
    private String appName;

    @Value("${xxl.job.executor.port:9999}")
    private int executorPort;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("XXL-Job 执行器初始化: admin={}, appname={}", adminAddresses, appName);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appName);
        executor.setAddress("");
        executor.setIp("");
        executor.setPort(executorPort);
        executor.setAccessToken(accessToken);
        executor.setLogPath("logs/xxl-job");
        executor.setLogRetentionDays(30);
        return executor;
    }
}
