package com.bite.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.system.domain.User;

/**
 * 用户表 Mapper。
 * <p>
 * 继承 {@link BaseMapper} 后可直接获得常用 CRUD：
 * selectById / selectList / insert / updateById / deleteById 等。
 */
public interface UserMapper extends BaseMapper<User> {
}

