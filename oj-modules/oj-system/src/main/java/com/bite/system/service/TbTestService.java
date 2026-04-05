package com.bite.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bite.system.domain.TbTest;

/**
 * tb_test 业务服务层。
 * <p>
 * 建议 Controller 通过 Service 访问数据层，避免直接依赖 Mapper，方便后续扩展事务/缓存/校验等逻辑。
 */
public interface TbTestService extends IService<TbTest> {

}

