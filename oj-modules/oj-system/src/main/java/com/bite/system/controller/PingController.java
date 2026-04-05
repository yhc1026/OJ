package com.bite.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最小化健康检查接口。
 * <p>
 * 用途：本地开发/联调时快速确认服务已启动、端口可用。
 */
@RestController
public class PingController {
    /**
     * 简单返回固定字符串，便于 curl / 浏览器直接验证。
     */
    @GetMapping("/ping")
    public String ping() {
        return "oj-system ok";
    }
}

