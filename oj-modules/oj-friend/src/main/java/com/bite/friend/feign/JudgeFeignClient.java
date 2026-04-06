package com.bite.friend.feign;

import com.bite.domain.Result;
import com.bite.friend.domain.dto.JudgeRunRequest;
import com.bite.friend.domain.dto.JudgeSingleCaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用 oj-judge 判题服务的 Feign 客户端。
 * <p>
 * 使用 friend 模块独立的 DTO，避免与 oj-judge 模块的类产生版本/序列化冲突。
 */
@FeignClient(
        name = "oj-judge",
        contextId = "ojJudgeFeign",
        path = "/judge"
)
public interface JudgeFeignClient {

    Logger log = LoggerFactory.getLogger(JudgeFeignClient.class);

    @PostMapping("/internal/run-case")
    Result<JudgeSingleCaseResponse> runOneCase(@RequestBody JudgeRunRequest request);
}
