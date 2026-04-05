package com.bite.friend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.friend.domain.FriendUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * C 端用户表 Mapper。
 */
@Mapper
public interface FriendUserMapper extends BaseMapper<FriendUser> {
}

