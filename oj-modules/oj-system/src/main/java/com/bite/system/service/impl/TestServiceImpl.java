package com.bite.system.service.impl;

import com.bite.system.domain.TbTest;
import com.bite.system.mapper.TestMapper;
import com.bite.system.service.TestService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 测试表（tb_test）Service 实现。
 * <p>
 * 该文件按你的命名要求创建：TestServiceImpl。
 */
@Service
public class TestServiceImpl implements TestService {
    private final TestMapper testMapper;

    public TestServiceImpl(TestMapper testMapper) {
        this.testMapper = testMapper;
    }

    @Override
    public boolean insert(TbTest tbTest) {
        Assert.notNull(tbTest, "tbTest must not be null");
        Assert.notNull(tbTest.getTestId(), "testId must not be null");
        Assert.hasText(tbTest.getTitle(), "title must not be blank");
        Assert.hasText(tbTest.getContent(), "content must not be blank");

        // 使用 MyBatis-Plus 默认插入（正常 SQL）
        return testMapper.insert(tbTest) == 1;
    }
}

