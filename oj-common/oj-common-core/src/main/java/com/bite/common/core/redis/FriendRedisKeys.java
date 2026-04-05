package com.bite.common.core.redis;

/**
 * C 端（friend）相关 Redis key 约定。
 * <p>
 * 命名：大驼峰语义段；带动态 id 的键使用 {@code 前缀-动态段}，多段 id 用 {@code -} 依次拼接。
 */
public final class FriendRedisKeys {

    private FriendRedisKeys() {
    }

    /** 邮箱登录验证码：{@code FriendEmailLoginVerificationCode-{email}} */
    public static final String LOGIN_CODE_PREFIX = "FriendEmailLoginVerificationCode-";

    /** 历史 C 端 token 详情（迁移清理用）：{@code FriendLegacyLoginPayloadByToken-{token}} */
    public static final String LOGIN_TOKEN_PREFIX = "FriendLegacyLoginPayloadByToken-";

    /** 历史 C 端 userId→token（迁移清理用）：{@code FriendLegacyActiveLoginTokenByUserId-{userId}} */
    public static final String LOGIN_ACTIVE_PREFIX = "FriendLegacyActiveLoginTokenByUserId-";

    /** 未开始+进行中竞赛 id 列表（Redis List），固定键名无动态段。 */
    public static final String EXAM_ACTIVE_IDS_KEY = "FriendExamListActiveIds";

    /** 已结束竞赛 id 列表（Redis List），固定键名无动态段。 */
    public static final String EXAM_FINISHED_IDS_KEY = "FriendExamListFinishedIds";

    /** 竞赛详情缓存：{@code FriendExamDetailCache-{examId}} */
    public static final String EXAM_DETAIL_PREFIX = "FriendExamDetailCache-";

    /** 某场竞赛题目 id 列表：{@code FriendExamQuestionIdList-{examId}} */
    public static final String EXAM_QUESTION_IDS_PREFIX = "FriendExamQuestionIdList-";

    /** 某场竞赛题目顺序列表（用户作答顺序）：{@code ExamQuestionsOrder-{examId}} */
    public static final String EXAM_QUESTION_ORDER_PREFIX = "ExamQuestionsOrder-";

    /** 题目详情缓存：{@code FriendQuestionDetailCache-{questionId}} */
    public static final String QUESTION_DETAIL_PREFIX = "FriendQuestionDetailCache-";

    /**
     * 用户已报名竞赛 id 列表（Redis List）：{@code FriendUserRegisteredExamIdList-{userId}}
     */
    public static final String USER_EXAM_LIST_PREFIX = "FriendUserRegisteredExamIdList-";

    /**
     * 用户是否已报名单场竞赛（String）：{@code FriendUserExamRegistrationFlag-{userId}-{examId}}
     */
    public static final String USER_EXAM_REGISTERED_PREFIX = "FriendUserExamRegistrationFlag-";

    public static String loginCodeKey(String email) {
        return LOGIN_CODE_PREFIX + email;
    }

    public static String loginTokenKey(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }

    public static String loginActiveKey(String userId) {
        return LOGIN_ACTIVE_PREFIX + userId;
    }

    public static String examQuestionListKey(String examId) {
        return EXAM_QUESTION_IDS_PREFIX + examId;
    }

    public static String examQuestionOrderKey(String examId) {
        return EXAM_QUESTION_ORDER_PREFIX + examId;
    }

    public static String questionDetailKey(String questionId) {
        return QUESTION_DETAIL_PREFIX + questionId;
    }

    public static String examDetailKey(String examId) {
        return EXAM_DETAIL_PREFIX + examId;
    }

    public static String userExamListKey(String userId) {
        return USER_EXAM_LIST_PREFIX + userId;
    }

    public static String userExamRegisteredKey(String userId, String examId) {
        return USER_EXAM_REGISTERED_PREFIX + userId + "-" + examId;
    }
}
