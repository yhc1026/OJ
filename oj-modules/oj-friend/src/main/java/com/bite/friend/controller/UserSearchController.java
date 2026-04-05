package com.bite.friend.controller;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.vo.ExamResponse;
import com.bite.friend.service.FriendSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户侧检索接口：题目/竞赛模糊查询（ES 优先，MySQL 回源）。
 */
@RestController
@RequestMapping("/friend/search")
public class UserSearchController {

    private final FriendSearchService friendSearchService;

    public UserSearchController(FriendSearchService friendSearchService) {
        this.friendSearchService = friendSearchService;
    }

    /** 1) 按题目 id 关键字模糊查询。 */
    @GetMapping("/question/by-id-like")
    public Result<List<FriendQuestion>> searchQuestionByIdLike(@RequestParam("questionId") String questionIdKeyword) {
        return friendSearchService.searchQuestionsByIdLike(questionIdKeyword);
    }

    /** 2) 按题目名称模糊查询。 */
    @GetMapping("/question/by-title-like")
    public Result<List<FriendQuestion>> searchQuestionByTitleLike(@RequestParam("title") String titleKeyword) {
        return friendSearchService.searchQuestionsByTitleLike(titleKeyword);
    }

    /** 3) 按难度查询题目列表（0/1/2）。 */
    @GetMapping("/question/by-difficulty")
    public Result<List<FriendQuestion>> listQuestionByDifficulty(@RequestParam("difficulty") Integer difficulty) {
        return friendSearchService.listQuestionsByDifficulty(difficulty);
    }

    /** 4) 按竞赛 id 关键字模糊查询。 */
    @GetMapping("/exam/by-id-like")
    public Result<List<ExamResponse>> searchExamByIdLike(@RequestParam("examId") String examIdKeyword) {
        return friendSearchService.searchExamsByIdLike(examIdKeyword);
    }

    /** 5) 按竞赛标题模糊查询。 */
    @GetMapping("/exam/by-title-like")
    public Result<List<ExamResponse>> searchExamByTitleLike(@RequestParam("title") String titleKeyword) {
        return friendSearchService.searchExamsByTitleLike(titleKeyword);
    }
}

