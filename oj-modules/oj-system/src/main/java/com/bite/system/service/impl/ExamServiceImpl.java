package com.bite.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.core.redis.FriendRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import com.bite.domain.Result;
import com.bite.system.domain.Exam;
import com.bite.system.domain.ExamQuestion;
import com.bite.system.domain.dto.ExamAddRequest;
import com.bite.system.mapper.ExamQuestionMapper;
import com.bite.system.mapper.ExamMapper;
import com.bite.system.service.ExamService;
import com.bite.system.support.SysUserOperatorVerifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {

    private static final int EXAM_PAGE_SIZE = 20;
    private static final int TITLE_MAX_LEN = 50;
    private final SysUserOperatorVerifier sysUserOperatorVerifier;
    private final ExamQuestionMapper examQuestionMapper;
    private final RedisOperatorService redisOperatorService;

    public ExamServiceImpl(SysUserOperatorVerifier sysUserOperatorVerifier,
                           ExamQuestionMapper examQuestionMapper,
                           RedisOperatorService redisOperatorService) {
        this.sysUserOperatorVerifier = sysUserOperatorVerifier;
        this.examQuestionMapper = examQuestionMapper;
        this.redisOperatorService = redisOperatorService;
    }

    private <T> Result<T> deny(Result<Void> v) {
        return new Result<>(v.getMsg(), v.getCode(), null);
    }

    @Override
    public Result<IPage<Exam>> list(long page) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        long current = Math.max(1L, page);
        IPage<Exam> data = page(new Page<>(current, EXAM_PAGE_SIZE));
        return Result.ok(ResultCode.SUCCESS.getMsg(), data);
    }

    @Override
    public Result<Exam> getExamById(Long examId) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (examId == null) {
            return new Result<>("examId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        Exam exam = getById(examId);
        if (exam == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), exam);
    }

    @Override
    public Result<Exam> getExamByName(String title) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (!StringUtils.hasText(title)) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        List<Exam> exams = list(Wrappers.<Exam>lambdaQuery().eq(Exam::getTitle, title.trim()));
        if (exams.isEmpty()) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        if (exams.size() > 1) {
            return new Result<>("存在同名竞赛，请使用 examId 查询", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), exams.get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> addExam(ExamAddRequest request) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        Long operatorId = sysUserOperatorVerifier.resolveOperatorUserId();
        if (operatorId == null) {
            return new Result<>("缺少操作人信息", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (request == null || request.getExam() == null) {
            return new Result<>("请求体不能为空，且必须包含 exam", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        Exam body = request.getExam();
        if (body.getExamId() != null) {
            return new Result<>("examId 由系统雪花算法生成，请勿手动传入", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (body == null || !StringUtils.hasText(body.getTitle())) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String title = body.getTitle().trim();
        if (title.length() > TITLE_MAX_LEN) {
            return new Result<>("title 长度不能超过 " + TITLE_MAX_LEN, ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        long duplicated = count(Wrappers.<Exam>lambdaQuery().eq(Exam::getTitle, title));
        if (duplicated > 0) {
            return new Result<>("竞赛名称已存在，不可重复", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (body.getStartTime() == null || body.getEndTime() == null) {
            return new Result<>("开始时间和结束时间不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!body.getStartTime().isAfter(now)) {
            return new Result<>("开始时间必须晚于当前时间", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (!body.getStartTime().isBefore(body.getEndTime())) {
            return new Result<>("开始时间必须早于结束时间", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        body.setTitle(title);
        body.setCreateBy(operatorId);
        body.setUpdateBy(operatorId);
        body.setCreateTime(now);
        body.setUpdateTime(now);
        boolean ok = save(body);
        if (!ok || body.getExamId() == null) {
            return Result.fail(ResultCode.FAILED);
        }

        List<Long> qids = request.getQuestionIdList();
        if (qids == null || qids.isEmpty()) {
            return new Result<>("questionIdList 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        AtomicInteger order = new AtomicInteger(1);
        for (Long qid : qids.stream().filter(v -> v != null).distinct().toList()) {
            ExamQuestion rel = new ExamQuestion();
            rel.setExamQuestionId(null);
            rel.setExamId(body.getExamId());
            rel.setQuestionId(qid);
            rel.setQuestionOrder(order.getAndIncrement());
            rel.setCreateBy(operatorId);
            rel.setUpdateBy(operatorId);
            rel.setCreateTime(now);
            rel.setUpdateTime(now);
            examQuestionMapper.insert(rel);
        }
        // 发布竞赛后，同步 Redis（examId->questionIdList）并清理该 exam 的详情缓存。
        syncExamQuestionListCache(body.getExamId(), qids);
        refreshExamListMembership(body.getExamId(), body.getStatus());
        evictExamDetailCache(body.getExamId());
        return Result.ok(ResultCode.SUCCESS.getMsg(), body.getExamId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteExamById(Long examId) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (examId == null) {
            return new Result<>("examId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        examQuestionMapper.delete(Wrappers.<ExamQuestion>lambdaQuery().eq(ExamQuestion::getExamId, examId));
        boolean ok = removeById(examId);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        // 删除竞赛后，立即清理 Redis 中对应竞赛缓存（不删除 exam list）。
        evictExamQuestionListCache(examId);
        removeExamIdFromBothLists(examId);
        evictExamDetailCache(examId);
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> deleteExamByName(String title) {
        Result<Void> g = sysUserOperatorVerifier.verifyOrFail();
        if (g != null) {
            return deny(g);
        }
        if (!StringUtils.hasText(title)) {
            return new Result<>("title 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String t = title.trim();
        List<Exam> exams = list(Wrappers.<Exam>lambdaQuery().eq(Exam::getTitle, t));
        long count = exams.size();
        if (count == 0) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        for (Exam exam : exams) {
            if (exam.getExamId() != null) {
                examQuestionMapper.delete(Wrappers.<ExamQuestion>lambdaQuery().eq(ExamQuestion::getExamId, exam.getExamId()));
                evictExamQuestionListCache(exam.getExamId());
                removeExamIdFromBothLists(exam.getExamId());
                evictExamDetailCache(exam.getExamId());
            }
        }
        boolean ok = remove(Wrappers.<Exam>lambdaQuery().eq(Exam::getTitle, t));
        if (!ok) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), (int) count);
    }

    private void syncExamQuestionListCache(Long examId, List<Long> rawQuestionIds) {
        if (examId == null) {
            return;
        }
        String examKey = FriendRedisKeys.examQuestionListKey(String.valueOf(examId));
        redisOperatorService.delete(examKey);
        if (rawQuestionIds == null || rawQuestionIds.isEmpty()) {
            return;
        }
        List<String> qids = rawQuestionIds.stream()
                .filter(v -> v != null)
                .distinct()
                .map(String::valueOf)
                .toList();
        if (!qids.isEmpty()) {
            redisOperatorService.rightPushAll(examKey, qids);
        }
    }

    private void evictExamQuestionListCache(Long examId) {
        if (examId == null) {
            return;
        }
        redisOperatorService.delete(FriendRedisKeys.examQuestionListKey(String.valueOf(examId)));
    }

    private void refreshExamListMembership(Long examId, Integer status) {
        if (examId == null) {
            return;
        }
        removeExamIdFromBothLists(examId);
        String id = String.valueOf(examId);
        if (status != null && status == 2) {
            redisOperatorService.rightPush(FriendRedisKeys.EXAM_FINISHED_IDS_KEY, id);
        } else {
            redisOperatorService.rightPush(FriendRedisKeys.EXAM_ACTIVE_IDS_KEY, id);
        }
    }

    private void removeExamIdFromBothLists(Long examId) {
        if (examId == null) {
            return;
        }
        String id = String.valueOf(examId);
        redisOperatorService.listRemove(FriendRedisKeys.EXAM_ACTIVE_IDS_KEY, 0, id);
        redisOperatorService.listRemove(FriendRedisKeys.EXAM_FINISHED_IDS_KEY, 0, id);
    }

    private void evictExamDetailCache(Long examId) {
        if (examId == null) {
            return;
        }
        redisOperatorService.delete(FriendRedisKeys.examDetailKey(String.valueOf(examId)));
    }
}

