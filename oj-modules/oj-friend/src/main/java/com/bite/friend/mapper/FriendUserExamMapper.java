package com.bite.friend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.friend.domain.FriendUserExam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-竞赛报名（tb_user_exam）Mapper。
 */
@Mapper
public interface FriendUserExamMapper extends BaseMapper<FriendUserExam> {
}
