package com.bite.friend.controller;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.dto.ExamRegisterRequest;
import com.bite.friend.domain.vo.ExamResponse;
import com.bite.friend.service.FriendExamService;
import com.bite.friend.service.FriendUserExamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端用户竞赛查询接口（需经网关鉴权）。
 * <p>
 * 当前提供两类列表查询，读取顺序为：
 * Redis 命中 -> 直接返回；
 * Redis 未命中 -> 回源 MySQL 并重建缓存。
 */
@RestController
@RequestMapping("/friend/exam")
public class UserExamController {

    private final FriendExamService friendExamService;
    private final FriendUserExamService friendUserExamService;

    public UserExamController(FriendExamService friendExamService,
                              FriendUserExamService friendUserExamService) {
        this.friendExamService = friendExamService;
        this.friendUserExamService = friendUserExamService;
    }

    /** 已结束竞赛列表（status=2）。 */
    @GetMapping("/list/finished")
    public Result<List<ExamResponse>> listFinishedExams() {
        return friendExamService.listFinishedExams();
    }

    /** 未开始+进行中竞赛列表（status in 0,1）。 */
    @GetMapping("/list/active")
    public Result<List<ExamResponse>> listActiveExams() {
        return friendExamService.listActiveExams();
    }

    /**
     * 单场竞赛详情（可经网关白名单匿名访问，便于访客浏览）。
     */
    @GetMapping("/detail")
    public Result<ExamResponse> getExamDetail(@RequestParam("examId") Long examId) {
        return friendExamService.getExamDetail(examId);
    }

    /**
     * 当前用户已报名的全部竞赛（需登录）。
     * <p>
     * 经网关校验 token 与 Redis 会话后，服务端用 token 解析 userId；报名 id 列表优先读 Redis {@code FriendUserRegisteredExamIdList-{userId}}，
     * 未命中再查 {@code tb_user_exam} 并回写 Redis。
     */
    @GetMapping("/my/registrations")
    public Result<List<ExamResponse>> listMyRegisteredExams(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader) {
        String token = extractToken(request);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        return friendUserExamService.listMyRegisteredExams(token, gatewayUserId);
    }

    /**
     * 报名竞赛：需携带 token；经网关时还会带有 {@code X-User-Id}（与网关 {@code LoginTokenFilter} 一致），服务端优先用 token 解析用户，必要时用请求头兜底。
     */
    @PostMapping("/registerExam")
    public Result<Long> registerExam(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader,
            @RequestBody ExamRegisterRequest body) {
        String token = extractToken(request);
        Long examId = body != null ? body.getExamId() : null;
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        return friendUserExamService.registerExam(token, examId, gatewayUserId);
    }

    /** 辅助接口：从MySQL查询题目ID列表并存入Redis */
    @GetMapping("/question/order/init")
    public Result<List<Long>> initQuestionOrder(@RequestParam("examId") Long examId) {
        return friendExamService.loadAndCacheExamQuestionOrder(examId);
    }

    /** 上一题 */
    @GetMapping("/question/prev")
    public Result<FriendQuestion> getPrevQuestion(
            @RequestParam("examId") Long examId,
            @RequestParam("questionId") Long questionId) {
        return friendExamService.getPrevQuestionId(examId, questionId);
    }

    /** 下一题 */
    @GetMapping("/question/next")
    public Result<FriendQuestion> getNextQuestion(
            @RequestParam("examId") Long examId,
            @RequestParam("questionId") Long questionId) {
        return friendExamService.getNextQuestionId(examId, questionId);
    }

    /** 获取竞赛第一题 */
    @GetMapping("/question/first")
    public Result<FriendQuestion> getFirstQuestion(@RequestParam("examId") Long examId) {
        return friendExamService.getFirstQuestion(examId);
    }

    /** 根据题目ID获取详情 */
    @GetMapping("/question/detail")
    public Result<FriendQuestion> getQuestionDetail(
            @RequestParam("examId") Long examId,
            @RequestParam("questionId") Long questionId) {
        return friendExamService.getQuestionById(examId, questionId);
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

    /** 与网关 {@link" com.bite.gateway.filter.LoginTokenFilter"} 提取顺序一致。 */
    private static String extractToken(HttpServletRequest request) {
        String t = request.getHeader("token");
        if (StringUtils.hasText(t)) {
            return t.trim();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        t = request.getParameter("token");
        return StringUtils.hasText(t) ? t.trim() : null;
    }
}

