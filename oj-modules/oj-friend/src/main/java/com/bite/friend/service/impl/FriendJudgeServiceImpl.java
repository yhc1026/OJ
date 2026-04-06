package com.bite.friend.service.impl;

import com.bite.common.core.enums.JudgeVerdict;
import com.bite.common.core.enums.ResultCode;
import com.bite.domain.Result;
import com.bite.friend.domain.FriendCodeSubmit;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.FriendUser;
import com.bite.friend.domain.dto.CodeSubmitRequest;
import com.bite.friend.domain.dto.JudgeRunRequest;
import com.bite.friend.domain.dto.JudgeSingleCaseResponse;
import com.bite.friend.domain.vo.CodeSubmitResultVo;
import com.bite.friend.feign.JudgeFeignClient;
import com.bite.friend.judge.QuestionCaseParser;
import com.bite.friend.judge.QuestionTestCase;
import com.bite.friend.mapper.FriendCodeSubmitMapper;
import com.bite.friend.service.FriendAuthService;
import com.bite.friend.service.FriendJudgeService;
import com.bite.friend.service.FriendQuestionForJudgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FriendJudgeServiceImpl implements FriendJudgeService {

    public static final int STATUS_PASS = 0;
    public static final int STATUS_FAIL = 1;
    public static final int STATUS_PENDING = 2;

    private static final int EXE_MSG_MAX = 1024;

    private final FriendAuthService friendAuthService;
    private final FriendQuestionForJudgeService friendQuestionForJudgeService;
    private final FriendCodeSubmitMapper friendCodeSubmitMapper;
    private final JudgeFeignClient judgeFeignClient;

    public FriendJudgeServiceImpl(FriendAuthService friendAuthService,
                                  FriendQuestionForJudgeService friendQuestionForJudgeService,
                                  FriendCodeSubmitMapper friendCodeSubmitMapper,
                                  JudgeFeignClient judgeFeignClient) {
        this.friendAuthService = friendAuthService;
        this.friendQuestionForJudgeService = friendQuestionForJudgeService;
        this.friendCodeSubmitMapper = friendCodeSubmitMapper;
        this.judgeFeignClient = judgeFeignClient;
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


        String qusetionCase=q.getQuestionCase();
        String expectedResult = q.getExpectedResult();


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
        vo.setSubmitId(row.getSubmitId());
        vo.setQuestionId(body.getQuestionId());
        vo.setQuestionCase(qusetionCase);

        long timeMs = q.getTimeLimit() == null ? 5000L : Math.max(500L, q.getTimeLimit());
        long spaceKb = q.getSpaceLimit() == null ? 262144L : Math.max(1024L, q.getSpaceLimit());

        StringBuilder exeSummary = new StringBuilder();

        // ========== 构建判题请求 ==========
        //todo expected output为空
        JudgeRunRequest request = new JudgeRunRequest();  // 创建判题请求对象
        request.setUserCode(body.getCode());              // 设置用户提交的代码
        request.setMainMethod(q.getMainMethod());         // 设置题目要求的入口方法（如 main 方法路径）
        request.setTestInput(q.getQuestionCase());        // 设置测试输入数据
        request.setExpectedOutput(q.getExpectedResult()); // 设置期望的输出结果（标准答案）
        request.setTimeLimitMs(timeMs);                   // 设置该题目的时间限制（毫秒）
        request.setSpaceLimitKb(spaceKb);                 // 设置该题目的内存限制（KB）
        request.setLanguage(body.getLanguage());          // 设置代码语言类型

        // ========== 初始化单个用例的结果对象 ==========
        CodeSubmitResultVo.ResultVo brief = new CodeSubmitResultVo.ResultVo();  // 结果容器
        brief.setExpectedOutput(expectedResult);                                 // 期望输出

        try {
            // ========== 调用判题服务（远程RPC调用）==========
            Result<JudgeSingleCaseResponse> jr = judgeFeignClient.runOneCase(request);
            System.out.println("feign 返回: code=" + (jr != null ? jr.getCode() : "null")
                    + ", data=" + (jr != null && jr.getData() != null ? jr.getData().toString() : "null"));

            // ========== 处理判题响应 ==========
            if (jr == null || jr.getCode() != ResultCode.SUCCESS.getCode()) {
                brief.setVerdict(JudgeVerdict.INTERNAL_ERROR.getCode());
                brief.setMessage(jr == null ? "判题服务无响应" : ("判题失败: " + jr.getMsg()));
                System.err.println("判题 Feign 调用失败, code=" + jr.getCode() + ", msg=" + jr.getMsg());
            } else if (jr.getData() == null) {
                brief.setVerdict(JudgeVerdict.INTERNAL_ERROR.getCode());
                brief.setMessage("判题结果为空");
                System.err.println("判题 Feign 返回 data 为空");
            } else {
                JudgeSingleCaseResponse data = jr.getData();
                brief.setVerdict(data.getVerdict());
                brief.setMessage(data.getMessage());
                brief.setActualOutput(trunc(data.getActualOutput(), 500));
                System.out.println("判题成功, verdict=" + data.getVerdict() + ", message=" + data.getMessage());
            }

        } catch (Exception e) {
            // ========== 捕获异常（调用失败）==========
            brief.setVerdict(JudgeVerdict.INTERNAL_ERROR.getCode());
            brief.setMessage("调用判题服务失败: " + e.getMessage());
            System.err.println("调用判题服务异常: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }


        // ========== 计算最终得分并更新数据库 ==========
        System.out.println(brief.getVerdict());
        int score = brief.getVerdict() == 0 ? 1 : 0;
        row.setScore(score);                                              // 设置得分到提交记录
        row.setStatus(score==1 ? STATUS_PASS : STATUS_FAIL);                 // 全部通过=0(通过)，否则=1(失败)
        row.setExeMessage(trunc(exeSummary.toString(), EXE_MSG_MAX));      // 截断执行消息（最多1024字符）
        row.setUpdateBy(userId);                                          // 设置更新人
        row.setUpdateTime(LocalDateTime.now());                           // 设置更新时间
        friendCodeSubmitMapper.updateById(row);                           // 持久化更新到数据库

        // ========== 组装返回结果 ==========
        vo.setStatus(row.getStatus());             // 设置状态到返回对象
        vo.setScore(row.getScore());               // 设置得分到返回对象
        vo.setExeMessage(row.getExeMessage());     // 设置执行消息到返回对象
        return Result.ok("success", vo);           // 返回成功结果
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
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
