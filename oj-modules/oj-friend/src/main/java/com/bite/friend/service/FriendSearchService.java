package com.bite.friend.service;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.vo.ExamResponse;

import java.util.List;

/**
 * 用户侧检索服务：
 * 1) 优先查 ES；
 * 2) ES 无结果时回源 MySQL。
 */
public interface FriendSearchService {

    Result<List<FriendQuestion>> searchQuestionsByIdLike(String questionIdKeyword);

    Result<List<FriendQuestion>> searchQuestionsByTitleLike(String titleKeyword);

    Result<List<FriendQuestion>> listQuestionsByDifficulty(Integer difficulty);

    Result<List<ExamResponse>> searchExamsByIdLike(String examIdKeyword);

    Result<List<ExamResponse>> searchExamsByTitleLike(String titleKeyword);
}

