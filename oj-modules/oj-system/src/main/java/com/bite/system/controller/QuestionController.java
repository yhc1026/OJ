package com.bite.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bite.domain.Result;
import com.bite.system.domain.Question;
import com.bite.system.domain.vo.QuestionBriefVo;
import com.bite.system.domain.vo.QuestionDetailVo;
import com.bite.system.service.QuestionService;
import com.bite.system.support.SysUserOperatorVerifier;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 题目管理接口（tb_question）。
 * <p>
 * 列表/概要/详情及按标题查询 brief、detail：白名单放行，无需 token。新增、删除：需 token，且业务层校验 {@code identity=sysUser}；
 * 经网关访问新增时会注入 {@code X-User-Id} 作为创建人。
 */
@RequestMapping("/question")
@RestController
public class QuestionController {

    private final QuestionService questionService;
    private final SysUserOperatorVerifier sysUserOperatorVerifier;

    public QuestionController(QuestionService questionService,
                              SysUserOperatorVerifier sysUserOperatorVerifier) {
        this.questionService = questionService;
        this.sysUserOperatorVerifier = sysUserOperatorVerifier;
    }

    /** 分页列表，每页 20 条 */
    @GetMapping("/page")
    public Result<IPage<QuestionBriefVo>> page(
            @RequestParam(value = "page", defaultValue = "1") long page) {
        return questionService.pageQuestions(page);
    }

    /** 按题目 id 查询概要：标题、难度、时间/空间限制（与 {@link QuestionBriefVo} 一致） */
    @GetMapping("/brief")
    public Result<QuestionBriefVo> brief(@RequestParam("questionId") Long questionId) {
        return questionService.getBriefByQuestionId(questionId);
    }

    /** 与 ExamController 同逻辑命名：分页查询题目（每页 20 条）。 */
    @GetMapping("/list")
    public Result<IPage<QuestionBriefVo>> list(
            @RequestParam(value = "page", defaultValue = "1") long page) {
        return questionService.pageQuestions(page);
    }

    /** 按题目 id 查询详情（含测试用例、默认代码、审计字段等，见 {@link QuestionDetailVo}） */
    @GetMapping("/detail")
    public Result<QuestionDetailVo> detail(@RequestParam("questionId") Long questionId) {
        return questionService.getDetailByQuestionId(questionId);
    }

    /** 与 ExamController 同逻辑命名：按 id 查询题目。 */
    @GetMapping("/getQuestionById")
    public Result<QuestionDetailVo> getQuestionById(@RequestParam("questionId") Long questionId) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return new Result<>(g.getMsg(), g.getCode(), null);
        }
        return questionService.getDetailByQuestionId(questionId);
    }

    /** 按题目名称（标题完全匹配）查询概要，白名单免 token */
    @GetMapping("/brief-by-title")
    public Result<QuestionBriefVo> briefByTitle(@RequestParam("title") String title) {
        return questionService.getBriefByTitle(title);
    }

    /** 按题目名称（标题完全匹配）查询详情，白名单免 token */
    @GetMapping("/detail-by-title")
    public Result<QuestionDetailVo> detailByTitle(@RequestParam("title") String title) {
        return questionService.getDetailByTitle(title);
    }

    /** 与 ExamController 同逻辑命名：按名称查询题目。 */
    @GetMapping("/getQuestionByName")
    public Result<QuestionDetailVo> getQuestionByName(@RequestParam("title") String title) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return new Result<>(g.getMsg(), g.getCode(), null);
        }
        return questionService.getDetailByTitle(title);
    }

    /** 按题目标题模糊查询列表（白名单免 token） */
    @GetMapping("/list-by-title-like")
    public Result<List<QuestionBriefVo>> listByTitleLike(@RequestParam("title") String title) {
        return questionService.listByTitleLike(title);
    }

    /**
     * 按难度标签查询题目列表：difficulty=easy|medium|hard。
     */
    @GetMapping("/list-by-difficulty")
    public Result<List<QuestionBriefVo>> listByDifficulty(@RequestParam("difficulty") String difficulty) {
        return questionService.listByDifficultyLabel(difficulty);
    }

    /** 新增题目（questionId 由雪花算法生成） */
    @PostMapping("/addQuestion")
    public Result<Long> add(@RequestBody Question body) {
        return questionService.addQuestion(body);
    }

    /** 根据题目名称删除（标题完全匹配，若重复则全部删除） */
    @DeleteMapping("/by-title")
    public Result<Integer> deleteByTitle(@RequestParam("title") String title) {
        return questionService.removeByTitle(title);
    }

    /** 与 ExamController 同逻辑命名：按题目名称删除。 */
    @DeleteMapping("/deleteQuestionByName")
    public Result<Integer> deleteQuestionByName(@RequestParam("title") String title) {
        return questionService.removeByTitle(title);
    }

    /** 根据题目 id 删除 */
    @DeleteMapping("/by-id")
    public Result<Boolean> deleteById(@RequestParam("questionId") Long questionId) {
        return questionService.removeByQuestionId(questionId);
    }

    /** 与 ExamController 同逻辑命名：按题目 id 删除。 */
    @DeleteMapping("/deleteQuestionById")
    public Result<Boolean> deleteQuestionById(@RequestParam("questionId") Long questionId) {
        return questionService.removeByQuestionId(questionId);
    }

    /**
     * 查询某竞赛下的题目列表（联表 exam + exam_question + question）。
     * 二选一入参：examId 或 examName。
     */
    @GetMapping("/getQuestionFromExam")
    public Result<List<QuestionBriefVo>> getQuestionFromExam(
            @RequestParam(value = "examId", required = false) Long examId,
            @RequestParam(value = "examName", required = false) String examName) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return new Result<>(g.getMsg(), g.getCode(), null);
        }
        return questionService.getQuestionFromExam(examId, examName);
    }

    /**
     * 修改题目接口没做，直接添加新题目，删除旧题目即可
     */
}
