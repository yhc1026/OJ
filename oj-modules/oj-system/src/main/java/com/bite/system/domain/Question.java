package com.bite.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import lombok.Data;

/**
 * 题目表实体，对应：tb_question。
 *
 * <pre>
 * CREATE TABLE tb_question (
 *   question_id bigint unsigned NOT NULL COMMENT '题目id',
 *   title varchar(32) NOT NULL COMMENT '标题',
 *   difficulty tinyint NOT NULL COMMENT '难度',
 *   time_limit int COMMENT '时间限制',
 *   space_limit int COMMENT '空间限制',
 *   content varchar(1024) NOT NULL COMMENT '题目内容',
 *   question_case varchar(1024) NOT NULL COMMENT '测试用例',
 *   default_code varchar(256) NOT NULL COMMENT '默认代码块',
 *   main_method varchar(256) NOT NULL COMMENT 'main方法',
 *   expected_result varchar(1024) COMMENT '期望输出',
 *   create_by bigint unsigned NOT NULL COMMENT '创建人',
 *   create_time datetime NOT NULL COMMENT '创建时间',
 *   update_by bigint unsigned COMMENT '更新人',
 *   update_time datetime COMMENT '更新时间',
 *   PRIMARY KEY (question_id)
 * );
 * </pre>
 */
@TableName("tb_question")
@Data
public class Question extends BaseEntity {

    /**
     * 题目 id（bigint unsigned）。
     * <p>
     * 表未声明自增，使用雪花等策略由应用赋值；若改库表为自增可改为 {@link IdType#AUTO}。
     */
    @TableId(value = "question_id", type = IdType.ASSIGN_ID)
    private Long questionId;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 难度（对应 tinyint） */
    @TableField("difficulty")
    private Integer difficulty;

    /** 时间限制（毫秒等含义由业务约定，可为空） */
    @TableField("time_limit")
    private Integer timeLimit;

    /** 空间限制（KB 等含义由业务约定，可为空） */
    @TableField("space_limit")
    private Integer spaceLimit;

    /** 题目内容 */
    @TableField("content")
    private String content;

    /** 测试用例 */
    @TableField("question_case")
    private String questionCase;

    /** 默认代码块 */
    @TableField("default_code")
    private String defaultCode;

    /** main 方法（签名或模板等，最长 256 与表字段一致） */
    @TableField("main_method")
    private String mainMethod;

    /** 期望输出 */
    @TableField("expected_result")
    private String expectedResult;
}
