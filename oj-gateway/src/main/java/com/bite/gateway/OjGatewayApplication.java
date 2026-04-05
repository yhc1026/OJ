package com.bite.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动入口。
 * <p>
 * 职责：作为系统对外统一入口（路由、鉴权、限流、跨域等通常放在这里）。
 */
@SpringBootApplication(scanBasePackages = "com.bite")
public class OjGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjGatewayApplication.class, args);
    }
}

