package com.bite.friend.service;

import com.bite.domain.Result;
import com.bite.friend.domain.vo.ExamResponse;

import java.util.List;

/**
 * 用户竞赛报名（tb_user_exam）与相关 Redis（如 {@code FriendUserRegisteredExamIdList-{userId}}）。
 */
public interface FriendUserExamService {

    /**
     * 当前用户报名指定竞赛：优先用 token 从 Redis 解析 userId；经网关时可带 {@code X-User-Id} 作兜底。
     *
     * @param token          登录 JWT
     * @param examId         竞赛 id
     * @param gatewayUserId  网关鉴权后注入的当前用户 id，可为 null
     */
    Result<Long> registerExam(String token, Long examId, Long gatewayUserId);

    /**
     * 当前登录用户已报名的全部竞赛：先读 Redis 列表 {@code FriendUserRegisteredExamIdList-{userId}}，
     * 未命中或为空则查 {@code tb_user_exam} 并回写 Redis。
     */
    Result<List<ExamResponse>> listMyRegisteredExams(String token, Long gatewayUserId);
}
