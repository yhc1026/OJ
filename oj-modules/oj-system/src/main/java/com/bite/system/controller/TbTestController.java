package com.bite.system.controller;

import com.bite.system.domain.TbTest;
import com.bite.system.service.TbTestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tb_test 表的演示接口（基于 MyBatis-Plus）。
 * <p>
 * 说明：这里提供最基础的 CRUD，便于快速验证“实体/Mapper/数据源/连接池”配置是否正确。
 */
@RestController
@RequestMapping("/tb-tests")
public class TbTestController {
    private final TbTestService tbTestService;

    public TbTestController(TbTestService tbTestService) {
        this.tbTestService = tbTestService;
    }

    /**
     * 查询全部（演示用；数据量大时请改为分页）。
     */
    @GetMapping
    public List<TbTest> list() {
        return tbTestService.list();
    }

    @GetMapping("/{id}")
    public TbTest get(@PathVariable("id") Long id) {
        return tbTestService.getById(id);
    }

    /**
     * 新增一条记录。
     * <p>
     * 注意：test_id 在表结构中未定义自增，这里要求请求体中携带 testId。
     */
    @PostMapping
    public boolean create(@RequestBody TbTest body) {
        return tbTestService.save(body);
    }

    /**
     * 根据主键更新。
     * <p>
     * 注意：请求体需要携带 testId 作为更新条件。
     */
    @PutMapping
    public boolean update(@RequestBody TbTest body) {
        return tbTestService.updateById(body);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return tbTestService.removeById(id);
    }
}

