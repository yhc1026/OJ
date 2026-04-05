package com.bite.friend.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 旧版竞赛列表 VO（保留兼容用）。
 */
@Data
public class FriendExamListItemVo {
    private String title;
    private Integer status;
    private String statusLabel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

