package com.bite.friend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bite.common.core.enums.ExamStatusEnum;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.core.redis.FriendRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.bite.friend.domain.FriendExam;
import com.bite.friend.domain.FriendUserExam;
import com.bite.friend.domain.vo.ExamResponse;
import com.bite.friend.mapper.FriendExamMapper;
import com.bite.friend.mapper.FriendUserExamMapper;
import com.bite.friend.service.FriendUserExamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FriendUserExamServiceImpl implements FriendUserExamService {

    private final FriendUserExamMapper friendUserExamMapper;
    private final FriendExamMapper friendExamMapper;
    private final RedisOperatorService redisOperatorService;
    private final LoginSessionRedisService loginSessionRedisService;
    private final ObjectMapper objectMapper;

    public FriendUserExamServiceImpl(FriendUserExamMapper friendUserExamMapper,
                                     FriendExamMapper friendExamMapper,
                                     RedisOperatorService redisOperatorService,
                                     LoginSessionRedisService loginSessionRedisService,
                                     ObjectMapper objectMapper) {
        this.friendUserExamMapper = friendUserExamMapper;
        this.friendExamMapper = friendExamMapper;
        this.redisOperatorService = redisOperatorService;
        this.loginSessionRedisService = loginSessionRedisService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> registerExam(String token, Long examId, Long gatewayUserId) {
        if (!StringUtils.hasText(token)) {
            return new Result<>("缺少 token", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        String t = token.trim();
        Long userId = resolveUserIdFromToken(t);
        if (userId == null && gatewayUserId != null && loginSessionRedisService.hasLoginSession(t)) {
            userId = gatewayUserId;
        }
        if (userId == null) {
            return new Result<>("登录态无效或已过期", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        if (examId == null) {
            return new Result<>("examId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }

        FriendExam exam = friendExamMapper.selectById(examId);
        if (exam == null) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }

        Integer examStatus = resolveExamStatusForRegister(examId, exam);
        if (examStatus == null) {
            return Result.fail(ResultCode.FAILED);
        }
        if (examStatus == ExamStatusEnum.FINISHED.getCode()) {
            return Result.fail("竞赛已结束");
        }
        if (examStatus == ExamStatusEnum.RUNNING.getCode()) {
            return Result.fail("竞赛进行中，不可报名");
        }
        if (examStatus != ExamStatusEnum.NOT_STARTED.getCode()) {
            return Result.fail("当前不可报名");
        }

        if (isUserAlreadyRegistered(userId, examId)) {
            return new Result<>("不可重复报名", ResultCode.FAILED_ALREADY_EXISTS.getCode(), null);
        }

        LocalDateTime now = LocalDateTime.now();
        FriendUserExam row = new FriendUserExam();
        row.setUserId(userId);
        row.setExamId(examId);
        row.setScore(null);
        row.setExamRank(null);
        row.setCreateBy(userId);
        row.setUpdateBy(userId);
        row.setCreateTime(now);
        row.setUpdateTime(now);

        int inserted = friendUserExamMapper.insert(row);
        if (inserted <= 0 || row.getUserExamId() == null) {
            return Result.fail(ResultCode.ERROR);
        }

        redisOperatorService.set(
                FriendRedisKeys.userExamRegisteredKey(String.valueOf(userId), String.valueOf(examId)),
                "1"
        );
        touchUserExamListCache(userId);
        return Result.ok(ResultCode.SUCCESS.getMsg(), row.getUserExamId());
    }

    @Override
    public Result<List<ExamResponse>> listMyRegisteredExams(String token, Long gatewayUserId) {
        if (!StringUtils.hasText(token)) {
            return new Result<>("缺少 token", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        String t = token.trim();
        Long userId = resolveUserIdFromToken(t);
        if (userId == null && gatewayUserId != null && loginSessionRedisService.hasLoginSession(t)) {
            userId = gatewayUserId;
        }
        if (userId == null) {
            return new Result<>("登录态无效或已过期", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }

        List<Long> examIds = loadRegisteredExamIds(userId);
        if (examIds.isEmpty()) {
            return Result.ok("success", List.of());
        }

        List<FriendExam> exams = friendExamMapper.selectBatchIds(examIds);
        Map<Long, FriendExam> byId = exams.stream()
                .filter(e -> e.getExamId() != null)
                .collect(Collectors.toMap(FriendExam::getExamId, e -> e, (a, b) -> a));
        List<ExamResponse> list = new ArrayList<>();
        for (Long eid : examIds) {
            FriendExam ex = byId.get(eid);
            if (ex != null) {
                list.add(toExamResponse(ex));
            }
        }
        return Result.ok("success", list);
    }

    /**
     * 先读 Redis List {@code FriendUserRegisteredExamIdList-{userId}}；无数据再查 {@code tb_user_exam} 并回写列表。
     */
    private List<Long> loadRegisteredExamIds(Long userId) {
        String key = FriendRedisKeys.userExamListKey(String.valueOf(userId));
        List<String> cached = redisOperatorService.listRange(key, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            List<Long> parsed = parseExamIdStrings(cached);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        List<FriendUserExam> rows = friendUserExamMapper.selectList(
                Wrappers.<FriendUserExam>lambdaQuery()
                        .select(FriendUserExam::getExamId)
                        .eq(FriendUserExam::getUserId, userId)
                        .orderByDesc(FriendUserExam::getCreateTime)
        );
        List<Long> fromDb = rows.stream()
                .map(FriendUserExam::getExamId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!fromDb.isEmpty()) {
            redisOperatorService.delete(key);
            redisOperatorService.rightPushAll(key, fromDb.stream().map(String::valueOf).toList());
        }
        return fromDb;
    }

    private static List<Long> parseExamIdStrings(List<String> cached) {
        List<Long> out = new ArrayList<>();
        for (String s : cached) {
            if (!StringUtils.hasText(s)) {
                continue;
            }
            try {
                out.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }

    private ExamResponse toExamResponse(FriendExam exam) {
        ExamResponse vo = new ExamResponse();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStatus(ExamStatusEnum.labelOf(exam.getStatus()));
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        return vo;
    }

    /**
     * 是否已报名：以数据库为准；Redis {@code FriendUserExamRegistrationFlag-{userId}-{examId}} 仅作加速。
     * <p>
     * 若仅 Redis 有标记、库中无记录（清库/手工删数据未删 key），会删掉脏标记，避免误判「已报名」。
     */
    private boolean isUserAlreadyRegistered(Long userId, Long examId) {
        String regKey = FriendRedisKeys.userExamRegisteredKey(String.valueOf(userId), String.valueOf(examId));
        long dup = friendUserExamMapper.selectCount(
                Wrappers.<FriendUserExam>lambdaQuery()
                        .eq(FriendUserExam::getUserId, userId)
                        .eq(FriendUserExam::getExamId, examId)
        );
        if (dup > 0) {
            redisOperatorService.set(regKey, "1");
            return true;
        }
        if (Boolean.TRUE.equals(redisOperatorService.hasKey(regKey))) {
            redisOperatorService.delete(regKey);
        }
        return false;
    }

    /**
     * 优先从 Redis 竞赛详情（{@link FriendRedisKeys#examDetailKey}）解析 status；未命中或解析失败则用库表中的 {@code exam.status}。
     */
    private Integer resolveExamStatusForRegister(Long examId, FriendExam examFromDb) {
        Integer fromCache = parseExamStatusFromRedisDetail(examId);
        if (fromCache != null) {
            return fromCache;
        }
        return examFromDb != null ? examFromDb.getStatus() : null;
    }

    /**
     * 详情 JSON 中 status 为枚举 label（not_started / running / finished）。
     */
    private Integer parseExamStatusFromRedisDetail(Long examId) {
        String raw = redisOperatorService.get(FriendRedisKeys.examDetailKey(String.valueOf(examId)));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode st = root.get("status");
            if (st == null || st.isNull()) {
                return null;
            }
            String label = st.asText();
            if (!StringUtils.hasText(label)) {
                return null;
            }
            for (ExamStatusEnum e : ExamStatusEnum.values()) {
                if (e.getLabel().equals(label.trim())) {
                    return e.getCode();
                }
            }
            return null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从 Redis {@code LoginSessionPayloadByToken-{token}} 解析用户 id，并与 {@code ActiveLoginTokenByUserId} 校验（与网关一致）。
     */
    private Long resolveUserIdFromToken(String token) {
        String payloadJson = loginSessionRedisService.getLoginPayload(token);
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode uid = root.get("userId");
            if (uid == null || uid.isNull()) {
                return null;
            }
            String userIdStr = uid.asText();
            if (!StringUtils.hasText(userIdStr)) {
                return null;
            }
            long userId = Long.parseLong(userIdStr.trim());
            if (!loginSessionRedisService.matchesActiveToken(userIdStr.trim(), token)) {
                return null;
            }
            return userId;
        } catch (JsonProcessingException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * 若已存在用户-竞赛 id 列表缓存则删除；若不存在则从库加载并写入该列表。
     */
    private void touchUserExamListCache(Long userId) {
        String key = FriendRedisKeys.userExamListKey(String.valueOf(userId));
        if (Boolean.TRUE.equals(redisOperatorService.hasKey(key))) {
            redisOperatorService.delete(key);
            return;
        }
        List<FriendUserExam> rows = friendUserExamMapper.selectList(
                Wrappers.<FriendUserExam>lambdaQuery()
                        .select(FriendUserExam::getExamId)
                        .eq(FriendUserExam::getUserId, userId)
        );
        List<String> examIds = rows.stream()
                .map(FriendUserExam::getExamId)
                .filter(id -> id != null)
                .map(String::valueOf)
                .toList();
        if (!examIds.isEmpty()) {
            redisOperatorService.rightPushAll(key, examIds);
        }
    }
}
