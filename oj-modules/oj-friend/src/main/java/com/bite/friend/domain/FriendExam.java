package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛实体（tb_exam）。
 * 用于 C 端竞赛列表查询与缓存构建。
 */
@TableName("tb_exam")
@Data
public class FriendExam {
    @TableId("exam_id")
    private Long examId;

    @TableField("title")
    private String title;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;
}

