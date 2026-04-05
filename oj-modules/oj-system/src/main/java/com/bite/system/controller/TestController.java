package com.bite.system.controller;

import com.bite.system.domain.TbTest;
import com.bite.system.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口（对应 tb_test 表）。
 * <p>
 * 该文件按你的命名要求创建：TestController。
 * 提供插入数据方法，用于验证 MyBatis-Plus + HikariCP 数据源链路是否正常。
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {
    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    /**
     * 插入一条 tb_test 记录。
     *
     * 请求示例：
     * <pre>
     * POST /test/insert
     * {
     *   "testId": 1,
     *   "title": "test",
     *   "content": "test"
     * }
     * </pre>
     */
    @PostMapping("/insert")
    public boolean insert(@RequestBody TbTest body) {
        return testService.insert(body);
    }

    @GetMapping("/log")
    public String logByQuery(@RequestParam("str") String str) {
        writeLogs(str);
        return "log test";
    }

    @PostMapping("/log")
    public String log(@RequestBody String str) {
        writeLogs(str);
        return "log test";
    }

    private void writeLogs(String str) {
        String infoMsg = "log info => " + str;
        String errorMsg = "log error => " + str + " false";
        // 日志框架输出
        log.info(infoMsg);
        log.error(errorMsg);
        // 直接标准输出，确保在 IDE 控制台可见
        System.out.println(infoMsg);
        System.err.println(errorMsg);
    }
}

