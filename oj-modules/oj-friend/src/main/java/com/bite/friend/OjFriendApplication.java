package com.bite.friend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 好友/社交服务启动入口。
 * <p>
 * 该服务通常承载：好友关系、关注、私信/通知等社交域能力（后续按课件逐步扩展）。
 */
@SpringBootApplication(scanBasePackages = "com.bite")
@EnableFeignClients(basePackages = "com.bite.friend.feign")
public class OjFriendApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjFriendApplication.class, args);
    }
}

