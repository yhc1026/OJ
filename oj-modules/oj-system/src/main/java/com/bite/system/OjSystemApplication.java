package com.bite.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
/**
 * 系统服务启动入口。
 * <p>
 * 该服务通常承载：用户、权限、角色、菜单等“系统域”能力（后续按课件逐步完善）。
 */
@SpringBootApplication(scanBasePackages = "com.bite")
@MapperScan("com.bite.**.mapper")
public class OjSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjSystemApplication.class, args);
    }
}

