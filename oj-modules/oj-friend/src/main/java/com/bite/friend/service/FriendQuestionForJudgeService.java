package com.bite.friend.service;

import com.bite.friend.domain.FriendQuestion;

/**
 * 判题场景加载题目：优先 ES，miss 回源 MySQL 并回填 ES。
 */
public interface FriendQuestionForJudgeService {

    FriendQuestion loadQuestion(Long questionId);
}
