package com.bite.friend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bite.common.core.enums.ExamStatusEnum;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.core.redis.FriendRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import com.bite.domain.Result;
import com.bite.friend.domain.FriendExam;
import com.bite.friend.domain.FriendExamQuestion;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.vo.ExamResponse;
import com.bite.friend.mapper.FriendExamMapper;
import com.bite.friend.mapper.FriendExamQuestionMapper;
import com.bite.friend.mapper.FriendQuestionMapper;
import com.bite.friend.service.FriendExamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 竞赛列表查询实现。
 * <p>
 * 查询策略：
 * 1) 先查 Redis 的 examId 列表与 examDetail；
 * 2) 未命中时回源 MySQL；
 * 3) 回源后重建 exam->question 关系缓存与 question 详情缓存。
 */
@Service
public class FriendExamServiceImpl implements FriendExamService {

    private final FriendExamMapper friendExamMapper;
    private final FriendExamQuestionMapper friendExamQuestionMapper;
    private final FriendQuestionMapper friendQuestionMapper;
    private final RedisOperatorService redisOperatorService;
    private final ObjectMapper objectMapper;

    public FriendExamServiceImpl(FriendExamMapper friendExamMapper,
                                 FriendExamQuestionMapper friendExamQuestionMapper,
                                 FriendQuestionMapper friendQuestionMapper,
                                 RedisOperatorService redisOperatorService,
                                 ObjectMapper objectMapper) {
        this.friendExamMapper = friendExamMapper;
        this.friendExamQuestionMapper = friendExamQuestionMapper;
        this.friendQuestionMapper = friendQuestionMapper;
        this.redisOperatorService = redisOperatorService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<List<ExamResponse>> listFinishedExams() {
        return queryByStatusSet(List.of(2), FriendRedisKeys.EXAM_FINISHED_IDS_KEY);
    }

    @Override
    public Result<List<ExamResponse>> listActiveExams() {
        return queryByStatusSet(List.of(0, 1), FriendRedisKeys.EXAM_ACTIVE_IDS_KEY);
    }

    @Override
    public Result<ExamResponse> getExamDetail(Long examId) {
        if (examId == null) {
            return Result.fail("examId 不能为空");
        }
        String idStr = String.valueOf(examId);
        ExamResponse vo;
        String raw = redisOperatorService.get(FriendRedisKeys.examDetailKey(idStr));
        if (StringUtils.hasText(raw)) {
            try {
                vo = objectMapper.readValue(raw, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                vo = null;
            }
            if (vo != null) {
                vo.setQuestionIds(loadQuestionIdsForExam(examId));
                return Result.ok("success", vo);
            }
        }
        FriendExam exam = friendExamMapper.selectById(examId);
        if (exam == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        vo = toResponse(exam);
        vo.setQuestionIds(loadQuestionIdsForExam(examId));
        return Result.ok("success", vo);
    }

    /** Redis List {@code FriendExamQuestionIdList-{examId}}，未命中则查 {@code tb_exam_question}。 */
    private List<Long> loadQuestionIdsForExam(Long examId) {
        String idStr = String.valueOf(examId);
        List<String> fromRedis = redisOperatorService.listRange(FriendRedisKeys.examQuestionListKey(idStr), 0, -1);
        if (fromRedis != null && !fromRedis.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (String s : fromRedis) {
                if (!StringUtils.hasText(s)) {
                    continue;
                }
                try {
                    ids.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        List<FriendExamQuestion> rels = friendExamQuestionMapper.selectList(
                Wrappers.<FriendExamQuestion>lambdaQuery()
                        .eq(FriendExamQuestion::getExamId, examId)
                        .orderByAsc(FriendExamQuestion::getQuestionOrder)
        );
        return rels.stream()
                .map(FriendExamQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Result<List<ExamResponse>> queryByStatusSet(List<Integer> statuses, String examIdsListKey) {
        Result<List<ExamResponse>> redisResult = tryLoadFromRedis(examIdsListKey);
        if (redisResult != null) {
            return redisResult;
        }

        List<FriendExam> exams = friendExamMapper.selectList(
                Wrappers.<FriendExam>lambdaQuery()
                        .in(FriendExam::getStatus, statuses)
                        .orderByDesc(FriendExam::getStartTime)
        );
        List<ExamResponse> result = exams.stream().map(this::toResponse).toList();
        rebuildQuestionCaches(exams);
        cacheExamIdsAndDetails(examIdsListKey, exams, result);
        return Result.ok("success", result);
    }

    private Result<List<ExamResponse>> tryLoadFromRedis(String examIdsListKey) {
        List<String> examIds = redisOperatorService.listRange(examIdsListKey, 0, -1);
        if (examIds == null || examIds.isEmpty()) {
            return null;
        }

        List<String> detailKeys = examIds.stream().map(FriendRedisKeys::examDetailKey).toList();
        List<String> jsonList = redisOperatorService.multiGet(detailKeys);
        if (jsonList == null || jsonList.size() != detailKeys.size()) {
            return null;
        }

        List<ExamResponse> result = new ArrayList<>();
        for (String raw : jsonList) {
            if (!StringUtils.hasText(raw)) {
                return null;
            }
            try {
                result.add(objectMapper.readValue(raw, new TypeReference<>() {
                }));
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return Result.ok("success", result);
    }

    private ExamResponse toResponse(FriendExam exam) {
        ExamResponse vo = new ExamResponse();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStatus(ExamStatusEnum.labelOf(exam.getStatus()));
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        return vo;
    }

    private void cacheExamIdsAndDetails(String examIdsListKey, List<FriendExam> exams, List<ExamResponse> responses) {
        redisOperatorService.delete(examIdsListKey);
        if (exams == null || exams.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < exams.size(); i++) {
            Long examId = exams.get(i).getExamId();
            if (examId == null) {
                continue;
            }
            String id = String.valueOf(examId);
            ids.add(id);
            try {
                redisOperatorService.set(FriendRedisKeys.examDetailKey(id), objectMapper.writeValueAsString(responses.get(i)));
            } catch (JsonProcessingException ignored) {
                // 单条详情缓存失败不影响主流程。
            }
        }
        if (!ids.isEmpty()) {
            redisOperatorService.rightPushAll(examIdsListKey, ids);
        }
    }

    /**
     * 重建两类缓存：
     * 1) examId -> questionId list（Redis List）；
     * 2) questionId -> questionJson（Redis String）。
     */
    private void rebuildQuestionCaches(List<FriendExam> exams) {
        List<Long> examIds = exams.stream()
                .map(FriendExam::getExamId)
                .filter(id -> id != null)
                .toList();
        if (examIds.isEmpty()) {
            return;
        }

        List<FriendExamQuestion> relations = friendExamQuestionMapper.selectList(
                Wrappers.<FriendExamQuestion>lambdaQuery()
                        .in(FriendExamQuestion::getExamId, examIds)
                        .orderByAsc(FriendExamQuestion::getExamId, FriendExamQuestion::getQuestionOrder)
        );

        Map<Long, List<String>> examQuestionMap = relations.stream()
                .filter(rel -> rel.getExamId() != null && rel.getQuestionId() != null)
                .collect(Collectors.groupingBy(
                        FriendExamQuestion::getExamId,
                        Collectors.mapping(rel -> String.valueOf(rel.getQuestionId()), Collectors.toList())
                ));

        for (Long examId : examIds) {
            String examKey = String.valueOf(examId);
            String cacheKey = FriendRedisKeys.examQuestionListKey(examKey);
            redisOperatorService.delete(cacheKey);
            List<String> qids = examQuestionMap.getOrDefault(examId, new ArrayList<>());
            if (!qids.isEmpty()) {
                redisOperatorService.rightPushAll(cacheKey, qids);
            }
        }

        List<Long> questionIds = relations.stream()
                .map(FriendExamQuestion::getQuestionId)
                .filter(id -> id != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
        if (questionIds.isEmpty()) {
            return;
        }

        List<FriendQuestion> questions = friendQuestionMapper.selectList(
                Wrappers.<FriendQuestion>lambdaQuery().in(FriendQuestion::getQuestionId, questionIds)
        );
        for (FriendQuestion question : questions) {
            if (question.getQuestionId() == null) {
                continue;
            }
            try {
                redisOperatorService.set(
                        FriendRedisKeys.questionDetailKey(String.valueOf(question.getQuestionId())),
                        objectMapper.writeValueAsString(question)
                );
            } catch (JsonProcessingException ignored) {
                // 单题缓存失败不影响主流程返回。
            }
        }
    }

    @Override
    public Result<List<Long>> loadAndCacheExamQuestionOrder(Long examId) {
        if (examId == null) {
            return Result.fail("examId不能为空");
        }
        String idStr = String.valueOf(examId);
        String redisKey = FriendRedisKeys.examQuestionOrderKey(idStr);
        List<String> cached = redisOperatorService.listRange(redisKey, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            List<Long> ids = cached.stream().map(Long::parseLong).toList();
            return Result.ok("success", ids);
        }
        List<FriendExamQuestion> rels = friendExamQuestionMapper.selectList(
                Wrappers.<FriendExamQuestion>lambdaQuery()
                        .eq(FriendExamQuestion::getExamId, examId)
                        .orderByAsc(FriendExamQuestion::getQuestionOrder)
        );
        if (rels.isEmpty()) {
            return Result.fail("该竞赛暂无题目");
        }
        List<String> qids = rels.stream().map(r -> String.valueOf(r.getQuestionId())).toList();
        redisOperatorService.delete(redisKey);
        redisOperatorService.rightPushAll(redisKey, qids);
        List<Long> result = qids.stream().map(Long::parseLong).toList();
        return Result.ok("success", result);
    }


    /** 获取竞赛第一题：自动处理缓存，未命中时回源MySQL。 */
    @Override
    public Result<FriendQuestion> getFirstQuestion(Long examId) {
        if (examId == null) {
            return Result.fail("examId不能为空");
        }
        String examIdStr = String.valueOf(examId);

        // 1. 查Redis题目顺序列表
        String orderKey = FriendRedisKeys.examQuestionOrderKey(examIdStr);
        List<String> cachedOrder = redisOperatorService.listRange(orderKey, 0, -1);
        List<Long> questionOrder;

        if (cachedOrder != null && !cachedOrder.isEmpty()) {
            questionOrder = cachedOrder.stream()
                    .filter(s -> !s.isBlank())
                    .map(Long::parseLong)
                    .toList();
        } else {
            // 2. 未命中，回源MySQL
            Result<List<Long>> orderResult = loadAndCacheExamQuestionOrder(examId);
            if (orderResult.getCode() != ResultCode.SUCCESS.getCode() || orderResult.getData() == null) {
                return Result.fail(orderResult.getMsg());
            }
            questionOrder = orderResult.getData();
            if (questionOrder.isEmpty()) {
                return Result.fail("该竞赛暂无题目");
            }
        }

        Long firstId = questionOrder.get(0);

        // 3. 查Redis题目详情
        String detailKey = FriendRedisKeys.questionDetailKey(String.valueOf(firstId));
        String cachedJson = redisOperatorService.get(detailKey);
        if (StringUtils.hasText(cachedJson)) {
            try {
                FriendQuestion q = objectMapper.readValue(cachedJson, FriendQuestion.class);
                return Result.ok("success", q);
            } catch (JsonProcessingException ignored) {
                // 解析失败，继续查MySQL
            }
        }

        // 4. 未命中，回源MySQL
        FriendQuestion question = friendQuestionMapper.selectById(firstId);
        if (question == null) {
            return Result.fail("题目不存在");
        }

        // 5. 同步写入Redis
        try {
            redisOperatorService.set(detailKey, objectMapper.writeValueAsString(question));
        } catch (JsonProcessingException ignored) {
            // 缓存失败不影响返回
        }

        return Result.ok("success", question);
    }

    /** 根据题目ID获取详情，优先读缓存。 */
    @Override
    public Result<FriendQuestion> getQuestionById(Long examId, Long questionId) {
        if (examId == null || questionId == null) {
            return Result.fail("参数不能为空");
        }
        String qidStr = String.valueOf(questionId);
        String cachedJson = redisOperatorService.get(FriendRedisKeys.questionDetailKey(qidStr));
        if (StringUtils.hasText(cachedJson)) {
            try {
                FriendQuestion q = objectMapper.readValue(cachedJson, FriendQuestion.class);
                return Result.ok("success", q);
            } catch (JsonProcessingException ignored) {
            }
        }
        FriendQuestion question = friendQuestionMapper.selectById(questionId);
        if (question == null) {
            return Result.fail("题目不存在");
        }
        //生成json
        try {
            redisOperatorService.set(FriendRedisKeys.questionDetailKey(qidStr), objectMapper.writeValueAsString(question));
        } catch (JsonProcessingException ignored) {
        }
        return Result.ok("success", question);
    }

    /** 上一题：返回题目详情 */
    @Override
    public Result<FriendQuestion> getPrevQuestionId(Long examId, Long currentQuestionId) {
        Result<List<Long>> orderResult = loadAndCacheExamQuestionOrder(examId);
        if (orderResult.getCode() != ResultCode.SUCCESS.getCode() || orderResult.getData() == null) {
            return Result.fail(orderResult.getMsg());
        }
        List<Long> questionIds = orderResult.getData();
        if(questionIds.isEmpty()) {
            return Result.fail("题目列表为空");
        }
        int idx = questionIds.indexOf(currentQuestionId);
        if (idx == 0) {
            return Result.fail("当前已是第一题");
        }
        if (idx < 0) {
            return Result.fail("本题不存在，内部错误");
        }
        Long prevId = questionIds.get(idx - 1);
        FriendQuestion question = friendQuestionMapper.selectById(prevId);
        if (question == null) {
            return Result.fail("题目不存在");
        }
        return Result.ok("success", question);
    }

    /** 下一题：返回题目详情 */
    @Override
    public Result<FriendQuestion> getNextQuestionId(Long examId, Long currentQuestionId) {
        Result<List<Long>> orderResult = loadAndCacheExamQuestionOrder(examId);
        if (orderResult.getCode() != ResultCode.SUCCESS.getCode() || orderResult.getData() == null) {
            return Result.fail(orderResult.getMsg());
        }
        List<Long> questionIds = orderResult.getData();
        if(questionIds.isEmpty()) {
            return Result.fail("题目列表为空");
        }
        int idx = questionIds.indexOf(currentQuestionId);
        if (idx < 0) {
            return Result.fail("本题不存在，内部错误");
        }
        if (idx >= questionIds.size() - 1) {
            return Result.fail("当前已是最后一题");
        }
        Long nextId = questionIds.get(idx + 1);
        FriendQuestion question = friendQuestionMapper.selectById(nextId);
        if (question == null) {
            return Result.fail("题目不存在");
        }
        return Result.ok("success", question);
    }

}

