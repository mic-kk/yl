package com.tcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TCM 平台单体主工程启动类。
 * 领域分包：com.tcm.common 公共层 / com.tcm.integration 适配层 / com.tcm.{user,goods,...} 业务域。
 */
@SpringBootApplication
public class TradePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradePlatformApplication.class, args);
    }
}
