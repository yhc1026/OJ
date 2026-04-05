package com.bite.job.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.common.core.redis.FriendRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import com.bite.job.domain.Exam;
import com.bite.job.mapper.ExamMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛状态按时间推进，并维护 C 端 Redis。
 * <ul>
 *   <li>未开始→进行中：更新 MySQL；删除该竞赛详情缓存 {@code FriendExamDetailCache-{id}}</li>
 *   <li>进行中→结束：更新 MySQL；删除该竞赛详情缓存；同时更新 active / finished 两个列表：
 *       先从两列表移除该 id，再写入 finished（与 {@code ExamServiceImpl#refreshExamListMembership} 一致）</li>
 * </ul>
 */
@Service
public class ExamStatusRefreshService extends ServiceImpl<ExamMapper, Exam> {

    private final RedisOperatorService redisOperatorService;

    public ExamStatusRefreshService(RedisOperatorService redisOperatorService) {
        this.redisOperatorService = redisOperatorService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshByTime() {
        LocalDateTime now = LocalDateTime.now();

        List<Long> startedIds = list(
                Wrappers.<Exam>lambdaQuery()
                        .select(Exam::getExamId)
                        .eq(Exam::getStatus, 0)
                        .le(Exam::getStartTime, now)
        ).stream().map(Exam::getExamId).filter(id -> id != null).toList();

        Exam startUpdate = new Exam();
        startUpdate.setStatus(1);
        startUpdate.setUpdateTime(now);
        update(
                startUpdate,
                Wrappers.<Exam>lambdaUpdate()
                        .eq(Exam::getStatus, 0)
                        .le(Exam::getStartTime, now)
        );
        for (Long examId : startedIds) {
            evictExamDetailCache(examId);
        }

        List<Long> finishedIds = list(
                Wrappers.<Exam>lambdaQuery()
                        .select(Exam::getExamId)
                        .eq(Exam::getStatus, 1)
                        .le(Exam::getEndTime, now)
        ).stream().map(Exam::getExamId).filter(id -> id != null).toList();

        Exam finishUpdate = new Exam();
        finishUpdate.setStatus(2);
        finishUpdate.setUpdateTime(now);
        update(
                finishUpdate,
                Wrappers.<Exam>lambdaUpdate()
                        .eq(Exam::getStatus, 1)
                        .le(Exam::getEndTime, now)
        );
        for (Long examId : finishedIds) {
            evictExamDetailCache(examId);
            moveExamIdFromActiveToFinishedLists(examId);
        }
    }

    /**
     * 竞赛从进行中变为已结束：active 列表去掉该 id，finished 列表追加该 id（先清两表中的重复项再归类）。
     */
    private void moveExamIdFromActiveToFinishedLists(Long examId) {
        if (examId == null) {
            return;
        }
        String id = String.valueOf(examId);
        redisOperatorService.listRemove(FriendRedisKeys.EXAM_ACTIVE_IDS_KEY, 0, id);
        redisOperatorService.listRemove(FriendRedisKeys.EXAM_FINISHED_IDS_KEY, 0, id);
        redisOperatorService.rightPush(FriendRedisKeys.EXAM_FINISHED_IDS_KEY, id);
    }

    private void evictExamDetailCache(Long examId) {
        if (examId == null) {
            return;
        }
        redisOperatorService.delete(FriendRedisKeys.examDetailKey(String.valueOf(examId)));
    }
}
