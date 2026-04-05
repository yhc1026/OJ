package com.bite.friend.service;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.vo.ExamResponse;

import java.util.List;

/**
 * C 端竞赛查询服务。
 */
public interface FriendExamService {
    /** 查询已结束竞赛列表（status=2）。 */
    Result<List<ExamResponse>> listFinishedExams();

    /** 查询未开始+进行中竞赛列表（status in 0,1）。 */
    Result<List<ExamResponse>> listActiveExams();

    /** 单场竞赛详情：优先 Redis 缓存，未命中回源 {@code tb_exam}。 */
    Result<ExamResponse> getExamDetail(Long examId);

    /** 辅助接口：根据examId从MySQL查询题目ID列表并缓存到Redis。 */
    Result<List<Long>> loadAndCacheExamQuestionOrder(Long examId);

    /** 上一题接口：根据examId和当前questionId获取上一题详情。 */
    Result<FriendQuestion> getPrevQuestionId(Long examId, Long currentQuestionId);

    /** 下一题接口：根据examId和当前questionId获取下一题详情。 */
    Result<FriendQuestion> getNextQuestionId(Long examId, Long currentQuestionId);

    /** 获取竞赛第一题：自动处理缓存，未命中时回源MySQL。 */
    Result<FriendQuestion> getFirstQuestion(Long examId);

    /** 根据题目ID获取题目详情，优先从竞赛缓存读取。 */
    Result<FriendQuestion> getQuestionById(Long examId, Long questionId);
}

