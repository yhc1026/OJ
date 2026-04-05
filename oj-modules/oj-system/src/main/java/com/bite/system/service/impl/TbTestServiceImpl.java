package com.bite.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.system.domain.TbTest;
import com.bite.system.mapper.TbTestMapper;
import com.bite.system.service.TbTestService;
import org.springframework.stereotype.Service;

/**
 * tb_test 业务服务实现。
 * <p>
 * 默认实现已包含常用 CRUD（save/getById/list/updateById/removeById 等）。
 */
@Service
public class TbTestServiceImpl extends ServiceImpl<TbTestMapper, TbTest> implements TbTestService {
}

