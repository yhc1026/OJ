package com.bite.friend.controller;

import com.bite.domain.Result;
import com.bite.friend.domain.dto.CodeSubmitRequest;
import com.bite.friend.domain.vo.CodeSubmitResultVo;
import com.bite.friend.service.FriendJudgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代码提交与 Docker 沙箱判题（经网关 /friend/judge/**）。
 */
@RestController
@RequestMapping("/friend/judge")
public class CodeJudgeController {

    private final FriendJudgeService friendJudgeService;

    public CodeJudgeController(FriendJudgeService friendJudgeService) {
        this.friendJudgeService = friendJudgeService;
    }

    @PostMapping("/submit")
    public Result<CodeSubmitResultVo> submit(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader,
            @RequestBody CodeSubmitRequest body) {
        return friendJudgeService.submit(request, xUserIdHeader, body);
    }
}
