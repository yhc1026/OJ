package com.bite.friend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.mq.message.JudgeRunTaskMessage;
import com.bite.domain.Result;
import com.bite.friend.domain.FriendCodeSubmit;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.FriendUser;
import com.bite.friend.domain.dto.CodeSubmitRequest;
import com.bite.friend.domain.vo.CodeSubmitResultVo;
import com.bite.friend.mapper.FriendCodeSubmitMapper;
import com.bite.friend.mq.JudgeTaskProducer;
import com.bite.friend.service.FriendAuthService;
import com.bite.friend.service.FriendJudgeService;
import com.bite.friend.service.FriendQuestionForJudgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FriendJudgeServiceImpl implements FriendJudgeService {

    public static final int STATUS_PASS = 0;
    public static final int STATUS_FAIL = 1;
    public static final int STATUS_PENDING = 2;

    private final FriendAuthService friendAuthService;
    private final FriendQuestionForJudgeService friendQuestionForJudgeService;
    private final FriendCodeSubmitMapper friendCodeSubmitMapper;
    private final JudgeTaskProducer judgeTaskProducer;

    public FriendJudgeServiceImpl(FriendAuthService friendAuthService,
                                  FriendQuestionForJudgeService friendQuestionForJudgeService,
                                  FriendCodeSubmitMapper friendCodeSubmitMapper,
                                  JudgeTaskProducer judgeTaskProducer) {
        this.friendAuthService = friendAuthService;
        this.friendQuestionForJudgeService = friendQuestionForJudgeService;
        this.friendCodeSubmitMapper = friendCodeSubmitMapper;
        this.judgeTaskProducer = judgeTaskProducer;
    }

    @Override
    public Result<CodeSubmitResultVo> submit(HttpServletRequest httpRequest, String xUserIdHeader, CodeSubmitRequest body) {
        if (body == null || body.getQuestionId() == null) {
            return Result.fail("questionId 不能为空");
        }
        if (!StringUtils.hasText(body.getCode())) {
            return Result.fail("code 不能为空");
        }
        int lang = body.getLanguage() == null ? 0 : body.getLanguage();
        if (lang != 0) {
            return Result.fail("暂仅支持 Java（language=0）");
        }

        String token = extractToken(httpRequest, null);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        Result<FriendUser> userResult = friendAuthService.getCurrentUserDetail(token, gatewayUserId);
        Long userId = userResult.getData().getUserId();

        FriendQuestion q = friendQuestionForJudgeService.loadQuestion(body.getQuestionId());
        if (q == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS, null);
        }
        if (!StringUtils.hasText(q.getMainMethod())) {
            return Result.fail("题目未配置 main_method，无法判题");
        }

        String qusetionCase = q.getQuestionCase();


        // 预写入sql记录
        LocalDateTime now = LocalDateTime.now();
        FriendCodeSubmit row = new FriendCodeSubmit();
        row.setUserId(userId);
        row.setQuestionId(body.getQuestionId());
        row.setExamId(body.getExamId());
        row.setUserCode(body.getCode());
        row.setLanguage(lang);
        row.setScore(0);
        row.setStatus(STATUS_PENDING);
        row.setExeMessage(null);
        row.setCreateBy(userId);
        row.setCreateTime(now);
        row.setUpdateBy(userId);
        row.setUpdateTime(now);
        friendCodeSubmitMapper.insert(row);

        CodeSubmitResultVo vo = new CodeSubmitResultVo();
        long timeMs = q.getTimeLimit() == null ? 5000L : Math.max(500L, q.getTimeLimit());
        long spaceKb = q.getSpaceLimit() == null ? 262144L : Math.max(1024L, q.getSpaceLimit());
        JudgeRunTaskMessage message = new JudgeRunTaskMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSubmitId(row.getSubmitId());
        message.setUserId(userId);
        message.setQuestionId(body.getQuestionId());
        message.setExamId(body.getExamId());
        message.setUserCode(body.getCode());
        message.setMainMethod(q.getMainMethod());
        message.setTestInput(q.getQuestionCase());
        message.setExpectedOutput(q.getExpectedResult());
        message.setTimeLimitMs(timeMs);
        message.setSpaceLimitKb(spaceKb);
        message.setLanguage(lang);
        judgeTaskProducer.send(message);

        vo.setStatus(row.getStatus());             // 设置状态到返回对象
        vo.setScore(row.getScore());               // 设置得分到返回对象
        vo.setExeMessage("任务已入队，等待判题");    // 设置执行消息到返回对象
        vo.setSubmitId(row.getSubmitId());
        vo.setQuestionId(body.getQuestionId());
        vo.setQuestionCase(qusetionCase);
        return Result.ok("success", vo);           // 返回成功结果
    }

    @Override
    public Result<CodeSubmitResultVo> getSubmitResult(HttpServletRequest httpRequest, String xUserIdHeader, Long submitId) {
        if (submitId == null) {
            return Result.fail("submitId 不能为空");
        }
        String token = extractToken(httpRequest, null);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        Result<FriendUser> userResult = friendAuthService.getCurrentUserDetail(token, gatewayUserId);
        Long userId = userResult.getData().getUserId();

        FriendCodeSubmit row = friendCodeSubmitMapper.selectOne(new LambdaQueryWrapper<FriendCodeSubmit>()
                .eq(FriendCodeSubmit::getSubmitId, submitId)
                .eq(FriendCodeSubmit::getUserId, userId));
        if (row == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS, null);
        }

        CodeSubmitResultVo vo = new CodeSubmitResultVo();
        vo.setSubmitId(row.getSubmitId());
        vo.setQuestionId(row.getQuestionId());
        vo.setStatus(row.getStatus());
        vo.setScore(row.getScore());
        vo.setExeMessage(row.getExeMessage());
        return Result.ok("success", vo);
    }

    private static String extractToken(HttpServletRequest request, String tokenParam) {
        String t = request.getHeader("token");
        if (StringUtils.hasText(t)) {
            return t.trim();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }
        String queryToken = request.getParameter("token");
        return StringUtils.hasText(queryToken) ? queryToken.trim() : null;
    }

    private static Long parseOptionalLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
