package com.bite.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bite.domain.Result;
import com.bite.system.domain.Question;
import com.bite.system.domain.vo.QuestionBriefVo;
import com.bite.system.domain.vo.QuestionDetailVo;
import java.util.List;

/**
 * 题目（tb_question）业务接口。
 */
public interface QuestionService extends IService<Question> {

    /** 分页列表，每页 20 条（公开只读） */
    Result<IPage<QuestionBriefVo>> pageQuestions(long page);

    /** 按题目 id 查询概要（标题、难度） */
    Result<QuestionBriefVo> getBriefByQuestionId(Long questionId);

    /** 按题目 id 查询详情 */
    Result<QuestionDetailVo> getDetailByQuestionId(Long questionId);

    /** 按题目名称（标题完全匹配）查询概要；多条同标题时返回参数错误提示 */
    Result<QuestionBriefVo> getBriefByTitle(String title);

    /** 按题目名称（标题完全匹配）查询详情；多条同标题时返回参数错误提示 */
    Result<QuestionDetailVo> getDetailByTitle(String title);

    /** 按题目名称模糊查询列表（标题 like） */
    Result<List<QuestionBriefVo>> listByTitleLike(String titleKeyword);

    /**
     * 按难度英文标签查询题目列表（easy/medium/hard）。
     */
    Result<List<QuestionBriefVo>> listByDifficultyLabel(String difficultyLabel);

    /** 新增题目（题目 id 由雪花生成） */
    Result<Long> addQuestion(Question body);

    /** 按标题删除（可能删除多条，若标题重复） */
    Result<Integer> removeByTitle(String title);

    /** 按题目 id 删除 */
    Result<Boolean> removeByQuestionId(Long questionId);

    /** 查询竞赛中的题目列表（按关系表题序返回简要信息） */
    Result<List<QuestionBriefVo>> getQuestionFromExam(Long examId, String examName);
}
