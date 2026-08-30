package com.tcm.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端：tcm.infrastructure.minio.enabled=true 时启用。
 * 约束：敏感文件服务端加密，禁止公网直链访问。
 */
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "minio.enabled", havingValue = "true")
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:}")
    private String accessKey;

    @Value("${minio.secret-key:}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
