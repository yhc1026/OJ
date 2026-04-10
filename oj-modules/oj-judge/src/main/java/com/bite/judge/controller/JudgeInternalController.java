package com.bite.judge.controller;

import com.bite.domain.Result;
import com.bite.judge.domain.dto.friend.JudgeRunRequest;
import com.bite.judge.domain.dto.friend.JudgeSingleCaseResponse;
import com.bite.judge.service.JudgeRunService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对内判题接口（由 oj-friend 经 Feign 调用，不经过网关暴露给前端）。
 */
@RestController
@RequestMapping("/judge")
public class JudgeInternalController {

    private final JudgeRunService judgeRunService;

    public JudgeInternalController(JudgeRunService judgeRunService) {
        this.judgeRunService = judgeRunService;
    }

    @PostMapping("/internal/run-case")
    public Result<JudgeSingleCaseResponse> runCase(@RequestBody JudgeRunRequest request) {
        return judgeRunService.run(request);
    }
}