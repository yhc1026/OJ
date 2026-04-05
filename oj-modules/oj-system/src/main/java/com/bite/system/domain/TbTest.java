package com.bite.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对应表：tb_test
 *
 * <pre>
 * CREATE TABLE `tb_test` (
 *   `test_id` bigint unsigned NOT NULL,
 *   `title`   text NOT NULL,
 *   `content` text NOT NULL,
 *   PRIMARY KEY (`test_id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 */
@TableName("tb_test")
@Data
public class TbTest {
    /**
     * 主键：test_id（bigint unsigned）
     * <p>
     * 该表主键未声明自增，通常由业务侧自行赋值（示例 SQL 中插入 test_id=1）。
     */
    @TableId(value = "test_id", type = IdType.INPUT)
    private Long testId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;
}

