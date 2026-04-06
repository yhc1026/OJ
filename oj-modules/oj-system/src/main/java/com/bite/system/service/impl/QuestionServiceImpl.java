package com.bite.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.common.core.enums.QuestionDifficultyEnum;
import com.bite.common.core.enums.ResultCode;
import com.bite.domain.Result;
import com.bite.system.domain.Exam;
import com.bite.system.domain.ExamQuestion;
import com.bite.system.domain.Question;
import com.bite.system.domain.vo.QuestionBriefVo;
import com.bite.system.domain.vo.QuestionDetailVo;
import com.bite.system.mapper.ExamMapper;
import com.bite.system.mapper.ExamQuestionMapper;
import com.bite.system.mapper.QuestionMapper;
import com.bite.system.service.QuestionService;
import com.bite.system.support.SysUserOperatorVerifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目业务实现：仅「新增」「删除」校验 {@link SysUserOperatorVerifier}（须为 sysUser）；
 * 列表/概要/详情为公开只读，不校验登录身份。
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    private static final int QUESTION_PAGE_SIZE = 20;
    private static final int TITLE_MAX_LEN = 32;
    private static final int CONTENT_MAX_LEN = 1024;
    private static final int CASE_MAX_LEN = 1024;
    private static final int DEFAULT_CODE_MAX_LEN = 256;
    private static final int MAIN_METHOD_MAX_LEN = 256;
    /** MySQL tinyint（有符号）取值范围 */
    private static final int DIFFICULTY_MIN = -128;
    private static final int DIFFICULTY_MAX = 127;

    private final SysUserOperatorVerifier sysUserOperatorVerifier;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;

    public QuestionServiceImpl(SysUserOperatorVerifier sysUserOperatorVerifier,
                               ExamMapper examMapper,
                               ExamQuestionMapper examQuestionMapper) {
        this.sysUserOperatorVerifier = sysUserOperatorVerifier;
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
    }

    private <T> Result<T> deny(Result<Void> v) {
        return new Result<>(v.getMsg(), v.getCode(), null);
    }

    @Override
    public Result<IPage<QuestionBriefVo>> pageQuestions(long page) {
        long current = Math.max(1L, page);
        IPage<Question> raw = page(new Page<>(current, QUESTION_PAGE_SIZE));
        IPage<QuestionBriefVo> voPage = raw.convert(this::toBriefVo);
        return Result.ok(ResultCode.SUCCESS.getMsg(), voPage);
    }

    @Override
    public Result<QuestionBriefVo> getBriefByQuestionId(Long questionId) {
        if (questionId == null) {
            return new Result<>("questionId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        Question q = getById(questionId);
        if (q == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toBriefVo(q));
    }

    @Override
    public Result<QuestionDetailVo> getDetailByQuestionId(Long questionId) {
        if (questionId == null) {
            return new Result<>("questionId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        Question q = getById(questionId);
        if (q == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toDetailVo(q));
    }

    @Override
    public Result<QuestionBriefVo> getBriefByTitle(String title) {
        Result<Question> rq = loadSingleQuestionByTitle(title);
        if (rq.getCode() != ResultCode.SUCCESS.getCode()) {
            return new Result<>(rq.getMsg(), rq.getCode(), null);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toBriefVo(rq.getData()));
    }

    @Override
    public Result<QuestionDetailVo> getDetailByTitle(String title) {
        Result<Question> rq = loadSingleQuestionByTitle(title);
        if (rq.getCode() != ResultCode.SUCCESS.getCode()) {
            return new Result<>(rq.getMsg(), rq.getCode(), null);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toDetailVo(rq.getData()));
    }

    @Override
    public Result<List<QuestionBriefVo>> listByTitleLike(String titleKeyword) {
        if (!StringUtils.hasText(titleKeyword)) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String keyword = titleKeyword.trim();
        if (keyword.length() > TITLE_MAX_LEN) {
            return new Result<>("标题长度超过限制", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        List<Question> data = list(
                Wrappers.<Question>lambdaQuery().like(Question::getTitle, keyword)
        );
        List<QuestionBriefVo> voList = data.stream().map(this::toBriefVo).toList();
        return Result.ok(ResultCode.SUCCESS.getMsg(), voList);
    }

    @Override
    public Result<List<QuestionBriefVo>> listByDifficultyLabel(String difficultyLabel) {
        QuestionDifficultyEnum difficultyEnum = QuestionDifficultyEnum.fromLabel(difficultyLabel);
        if (difficultyEnum == null) {
            return new Result<>(
                    "difficulty 仅支持 easy、medium、hard",
                    ResultCode.FAILED_PARAMS_VALIDATE.getCode(),
                    null);
        }
        List<Question> data = list(
                Wrappers.<Question>lambdaQuery().eq(Question::getDifficulty, difficultyEnum.getCode())
        );
        List<QuestionBriefVo> voList = data.stream().map(this::toBriefVo).toList();
        return Result.ok(ResultCode.SUCCESS.getMsg(), voList);
    }

    /**
     * 按标题完全匹配查一条题目；无记录 / 多条记录均返回业务 {@link Result}（非成功码）。
     */
    private Result<Question> loadSingleQuestionByTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String t = title.trim();
        if (t.length() > TITLE_MAX_LEN) {
            return new Result<>("标题长度超过限制", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        List<Question> list = list(Wrappers.<Question>lambdaQuery().eq(Question::getTitle, t));
        if (list.isEmpty()) {
            return new Result<>(
                    ResultCode.FAILED_NOT_EXISTS.getMsg(),
                    ResultCode.FAILED_NOT_EXISTS.getCode(),
                    null);
        }
        if (list.size() > 1) {
            return new Result<>(
                    "存在多条相同标题的题目，请使用题目 id 查询",
                    ResultCode.FAILED_PARAMS_VALIDATE.getCode(),
                    null);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), list.get(0));
    }

    @Override
    public Result<Long> addQuestion(Question body) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        Long operatorId = sysUserOperatorVerifier.resolveOperatorUserId();
        if (operatorId == null) {
            return new Result<>(
                    "无法解析操作人 userId：请经网关访问（自动注入 X-User-Id），或确保登录态 Redis JSON 中含 userId",
                    ResultCode.FAILED_PARAMS_VALIDATE.getCode(),
                    null);
        }
        if (body == null) {
            return new Result<>("请求体不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (body.getQuestionId() != null) {
            return new Result<>("questionId 由系统雪花算法生成，请勿手动传入", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        try {
            validateForInsert(body);
        } catch (IllegalArgumentException ex) {
            return new Result<>(ex.getMessage(), ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        long duplicated = count(Wrappers.<Question>lambdaQuery().eq(Question::getTitle, body.getTitle()));
        if (duplicated > 0) {
            return new Result<>("题目名称已存在，不可重复", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }

        LocalDateTime now = LocalDateTime.now();
        body.setCreateBy(operatorId);
        body.setUpdateBy(operatorId);
        body.setCreateTime(now);
        body.setUpdateTime(now);

        boolean ok = save(body);
        if (!ok || body.getQuestionId() == null) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), body.getQuestionId());
    }

    @Override
    public Result<Integer> removeByTitle(String title) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (!StringUtils.hasText(title)) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String t = title.trim();
        if (t.length() > TITLE_MAX_LEN) {
            return new Result<>("标题长度超过限制", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        long count = count(Wrappers.<Question>lambdaQuery().eq(Question::getTitle, t));
        if (count == 0) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        boolean removed = remove(Wrappers.<Question>lambdaQuery().eq(Question::getTitle, t));
        if (!removed) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), (int) count);
    }

    @Override
    public Result<Boolean> removeByQuestionId(Long questionId) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (questionId == null) {
            return new Result<>("questionId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        boolean ok = removeById(questionId);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    @Override
    public Result<List<QuestionBriefVo>> getQuestionFromExam(Long examId, String examName) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        Long resolvedExamId = examId;
        if (resolvedExamId == null) {
            if (!StringUtils.hasText(examName)) {
                return new Result<>("examId 与 examName 不能同时为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
            }
            String name = examName.trim();
            List<Exam> exams = examMapper.selectList(
                    Wrappers.<Exam>lambdaQuery().eq(Exam::getTitle, name)
            );
            if (exams.isEmpty()) {
                return Result.fail(ResultCode.FAILED_NOT_EXISTS);
            }
            if (exams.size() > 1) {
                return new Result<>("存在同名竞赛，请改用 examId 查询", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
            }
            resolvedExamId = exams.get(0).getExamId();
        }

        List<ExamQuestion> relList = examQuestionMapper.selectList(
                Wrappers.<ExamQuestion>lambdaQuery()
                        .eq(ExamQuestion::getExamId, resolvedExamId)
                        .orderByAsc(ExamQuestion::getQuestionOrder)
                        .orderByAsc(ExamQuestion::getExamQuestionId)
        );
        if (relList.isEmpty()) {
            return Result.ok(ResultCode.SUCCESS.getMsg(), List.of());
        }

        List<Long> qids = relList.stream()
                .map(ExamQuestion::getQuestionId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (qids.isEmpty()) {
            return Result.ok(ResultCode.SUCCESS.getMsg(), List.of());
        }

        List<Question> questions = listByIds(qids);
        Map<Long, Question> questionMap = new LinkedHashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getQuestionId(), q);
        }

        List<QuestionBriefVo> result = new ArrayList<>();
        for (ExamQuestion rel : relList) {
            Question q = questionMap.get(rel.getQuestionId());
            if (q != null) {
                result.add(toBriefVo(q));
            }
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), result);
    }

    private void validateForInsert(Question body) {
        if (!StringUtils.hasText(body.getTitle()) || body.getTitle().trim().length() > TITLE_MAX_LEN) {
            throw new IllegalArgumentException("标题不能为空且长度不超过 " + TITLE_MAX_LEN);
        }
        body.setTitle(body.getTitle().trim());
        if (body.getDifficulty() == null) {
            throw new IllegalArgumentException("难度不能为空");
        }
        int diff = body.getDifficulty();
        if (diff < DIFFICULTY_MIN || diff > DIFFICULTY_MAX) {
            throw new IllegalArgumentException(
                    "难度须在 " + DIFFICULTY_MIN + "~" + DIFFICULTY_MAX + " 之间（对应 tinyint）");
        }
        if (!StringUtils.hasText(body.getContent()) || body.getContent().length() > CONTENT_MAX_LEN) {
            throw new IllegalArgumentException("题目内容不能为空且长度不超过 " + CONTENT_MAX_LEN);
        }
        if (!StringUtils.hasText(body.getQuestionCase()) || body.getQuestionCase().length() > CASE_MAX_LEN) {
            throw new IllegalArgumentException("测试用例不能为空且长度不超过 " + CASE_MAX_LEN);
        }
        if (!StringUtils.hasText(body.getDefaultCode()) || body.getDefaultCode().length() > DEFAULT_CODE_MAX_LEN) {
            throw new IllegalArgumentException("默认代码块不能为空且长度不超过 " + DEFAULT_CODE_MAX_LEN);
        }
        if (!StringUtils.hasText(body.getMainMethod()) || body.getMainMethod().length() > MAIN_METHOD_MAX_LEN) {
            throw new IllegalArgumentException("main 方法不能为空且长度不超过 " + MAIN_METHOD_MAX_LEN);
        }
        if (body.getTimeLimit() != null && body.getTimeLimit() < 0) {
            throw new IllegalArgumentException("时间限制不能为负数");
        }
        if (body.getSpaceLimit() != null && body.getSpaceLimit() < 0) {
            throw new IllegalArgumentException("空间限制不能为负数");
        }
    }

    private QuestionBriefVo toBriefVo(Question q) {
        QuestionBriefVo vo = new QuestionBriefVo();
        vo.setQuestionId(q.getQuestionId());
        vo.setTitle(q.getTitle());
        vo.setDifficulty(q.getDifficulty());
        vo.setDifficultyLabel(QuestionDifficultyEnum.labelOf(q.getDifficulty()));
        vo.setTimeLimit(q.getTimeLimit());
        vo.setSpaceLimit(q.getSpaceLimit());
        return vo;
    }

    private QuestionDetailVo toDetailVo(Question q) {
        QuestionDetailVo vo = new QuestionDetailVo();
        vo.setQuestionId(q.getQuestionId());
        vo.setTitle(q.getTitle());
        vo.setDifficulty(q.getDifficulty());
        vo.setDifficultyLabel(QuestionDifficultyEnum.labelOf(q.getDifficulty()));
        vo.setTimeLimit(q.getTimeLimit());
        vo.setSpaceLimit(q.getSpaceLimit());
        vo.setContent(q.getContent());
        vo.setQuestionCase(q.getQuestionCase());
        vo.setDefaultCode(q.getDefaultCode());
        vo.setMainMethod(q.getMainMethod());
        vo.setExpectedResult(q.getExpectedResult());
        vo.setCreateBy(q.getCreateBy());
        vo.setCreateTime(q.getCreateTime());
        vo.setUpdateBy(q.getUpdateBy());
        vo.setUpdateTime(q.getUpdateTime());
        return vo;
    }
}
