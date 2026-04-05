package com.bite.friend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.friend.domain.FriendQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目表 Mapper（用于构建 question 缓存）。
 */
@Mapper
public interface FriendQuestionMapper extends BaseMapper<FriendQuestion> {
}

