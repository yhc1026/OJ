package com.bite.system.service;

import com.bite.system.domain.TbTest;

/**
 * 测试表（tb_test）Service。
 * <p>
 * 该文件按你的命名要求创建：TestService。
 * 这里先聚焦于“插入数据”能力，后续可按需要扩展查询/更新/删除等方法。
 */
public interface TestService {
    /**
     * 插入一条 tb_test 记录。
     *
     * @param tbTest 待插入数据（需要携带 testId/title/content）
     * @return 是否插入成功
     */
    boolean insert(TbTest tbTest);
}

