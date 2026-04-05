package com.bite.friend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.friend.domain.FriendExamQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 竞赛-题目关系表 Mapper。
 */
@Mapper
public interface FriendExamQuestionMapper extends BaseMapper<FriendExamQuestion> {
}

