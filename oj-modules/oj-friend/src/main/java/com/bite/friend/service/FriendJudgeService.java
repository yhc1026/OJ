package com.bite.friend.service;

import com.bite.domain.Result;
import com.bite.friend.domain.dto.CodeSubmitRequest;
import com.bite.friend.domain.vo.CodeSubmitResultVo;
import jakarta.servlet.http.HttpServletRequest;

public interface FriendJudgeService {

    Result<CodeSubmitResultVo> submit(HttpServletRequest httpRequest,
                                      String xUserIdHeader,
                                      CodeSubmitRequest body);

    Result<CodeSubmitResultVo> getSubmitResult(HttpServletRequest httpRequest,
                                               String xUserIdHeader,
                                               Long submitId);
}
