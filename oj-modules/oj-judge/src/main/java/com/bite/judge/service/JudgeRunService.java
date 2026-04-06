package com.bite.judge.service;

import com.bite.domain.Result;
import com.bite.judge.domain.dto.friend.JudgeRunRequest;
import com.bite.judge.domain.dto.friend.JudgeSingleCaseResponse;

/**
 * 对内判题服务（供 friend 模块通过 Feign 调用）。
 */
public interface JudgeRunService {

    /**
     * 运行单个用例判题。
     *
     * @param request 判题请求
     * @return 判题结果
     */
    Result<JudgeSingleCaseResponse> run(JudgeRunRequest request);
}