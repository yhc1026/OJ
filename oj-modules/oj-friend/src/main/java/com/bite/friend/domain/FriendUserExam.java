package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-竞赛关联（成绩/排名）表，对应 {@code tb_user_exam}。
 */
@TableName("tb_user_exam")
@Data
public class FriendUserExam {

    @TableId(value = "user_exam_id", type = IdType.ASSIGN_ID)
    private Long userExamId;

    @TableField("user_id")
    private Long userId;

    @TableField("exam_id")
    private Long examId;

    /** 得分，可为空 */
    @TableField("score")
    private Integer score;

    /** 排名，对应列名 exam_rank */
    @TableField("exam_rank")
    private Integer examRank;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
