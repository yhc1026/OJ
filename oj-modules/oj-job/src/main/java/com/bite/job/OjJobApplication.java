package com.bite.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 任务服务启动入口。
 * <p>
 * 该服务通常承载：定时任务、异步任务、批处理等（后续可接入 Quartz / XXL-JOB 等）。
 */
@SpringBootApplication(scanBasePackages = "com.bite")
@MapperScan("com.bite.job.mapper")
public class OjJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjJobApplication.class, args);
    }
}

