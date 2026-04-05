package com.bite.judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 判题服务启动入口。
 * <p>
 * 该服务通常承载：提交评测、编译运行、结果汇总等判题相关能力（后续再接入沙箱/队列等）。
 */
@SpringBootApplication(scanBasePackages = "com.bite")
public class OjJudgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjJudgeApplication.class, args);
    }
}

