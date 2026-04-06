package com.bite.friend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bite.common.core.enums.ExamStatusEnum;
import com.bite.common.elasticsearch.exam.doc.ExamDoc;
import com.bite.common.elasticsearch.exam.repo.ExamEsRepository;
import com.bite.common.elasticsearch.question.doc.QuestionDoc;
import com.bite.common.elasticsearch.question.repo.QuestionEsRepository;
import com.bite.domain.Result;
import com.bite.friend.domain.FriendExam;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.domain.vo.ExamResponse;
import com.bite.friend.mapper.FriendExamMapper;
import com.bite.friend.mapper.FriendQuestionMapper;
import com.bite.friend.service.FriendSearchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户侧题目/竞赛模糊检索：
 * 先查 ES，未命中回源 MySQL。
 */
@Service
public class FriendSearchServiceImpl implements FriendSearchService {

    private final QuestionEsRepository questionEsRepository;
    private final ExamEsRepository examEsRepository;
    private final FriendQuestionMapper friendQuestionMapper;
    private final FriendExamMapper friendExamMapper;

    public FriendSearchServiceImpl(QuestionEsRepository questionEsRepository,
                                   ExamEsRepository examEsRepository,
                                   FriendQuestionMapper friendQuestionMapper,
                                   FriendExamMapper friendExamMapper) {
        this.questionEsRepository = questionEsRepository;
        this.examEsRepository = examEsRepository;
        this.friendQuestionMapper = friendQuestionMapper;
        this.friendExamMapper = friendExamMapper;
    }

    @Override
    public Result<List<FriendQuestion>> searchQuestionsByIdLike(String questionIdKeyword) {
        if (!StringUtils.hasText(questionIdKeyword)) {
            return Result.fail("questionId 关键字不能为空");
        }
        String kw = questionIdKeyword.trim();

        List<FriendQuestion> fromEs = mapQuestionDocs(questionEsRepository.findByIdContaining(kw));
        if (!fromEs.isEmpty()) {
            return Result.ok("success(es)", fromEs);
        }

        List<FriendQuestion> fromDb = friendQuestionMapper.selectList(
                Wrappers.<FriendQuestion>lambdaQuery()
                        .apply("CAST(question_id AS CHAR) LIKE {0}", "%" + kw + "%")
                        .orderByDesc(FriendQuestion::getQuestionId)
        );
        syncQuestionsToEsQuietly(fromDb);
        return Result.ok("success(mysql)", fromDb);
    }

    @Override
    public Result<List<FriendQuestion>> searchQuestionsByTitleLike(String titleKeyword) {
        if (!StringUtils.hasText(titleKeyword)) {
            return Result.fail("title 关键字不能为空");
        }
        String kw = titleKeyword.trim();

        List<FriendQuestion> fromEs = mapQuestionDocs(questionEsRepository.findByTitleContaining(kw));
        if (!fromEs.isEmpty()) {
            return Result.ok("success(es)", fromEs);
        }

        List<FriendQuestion> fromDb = friendQuestionMapper.selectList(
                Wrappers.<FriendQuestion>lambdaQuery()
                        .like(FriendQuestion::getTitle, kw)
                        .orderByDesc(FriendQuestion::getQuestionId)
        );
        syncQuestionsToEsQuietly(fromDb);
        return Result.ok("success(mysql)", fromDb);
    }

    @Override
    public Result<List<FriendQuestion>> listQuestionsByDifficulty(Integer difficulty) {
        if (difficulty == null || difficulty < 0 || difficulty > 2) {
            return Result.fail("difficulty 必须为 0/1/2");
        }

        List<FriendQuestion> fromEs = mapQuestionDocs(questionEsRepository.findByDifficulty(difficulty));
        if (!fromEs.isEmpty()) {
            return Result.ok("success(es)", fromEs);
        }

        List<FriendQuestion> fromDb = friendQuestionMapper.selectList(
                Wrappers.<FriendQuestion>lambdaQuery()
                        .eq(FriendQuestion::getDifficulty, difficulty)
                        .orderByDesc(FriendQuestion::getQuestionId)
        );
        syncQuestionsToEsQuietly(fromDb);
        return Result.ok("success(mysql)", fromDb);
    }

    @Override
    public Result<List<ExamResponse>> searchExamsByIdLike(String examIdKeyword) {
        if (!StringUtils.hasText(examIdKeyword)) {
            return Result.fail("examId 关键字不能为空");
        }
        String kw = examIdKeyword.trim();

        List<ExamResponse> fromEs = mapExamDocs(examEsRepository.findByIdContaining(kw));
        if (!fromEs.isEmpty()) {
            return Result.ok("success(es)", fromEs);
        }

        List<FriendExam> fromDb = friendExamMapper.selectList(
                Wrappers.<FriendExam>lambdaQuery()
                        .apply("CAST(exam_id AS CHAR) LIKE {0}", "%" + kw + "%")
                        .orderByDesc(FriendExam::getStartTime)
        );
        syncExamsToEsQuietly(fromDb);
        return Result.ok("success(mysql)", fromDb.stream().map(this::toExamResponse).toList());
    }

    @Override
    public Result<List<ExamResponse>> searchExamsByTitleLike(String titleKeyword) {
        if (!StringUtils.hasText(titleKeyword)) {
            return Result.fail("title 关键字不能为空");
        }
        String kw = titleKeyword.trim();

        List<ExamResponse> fromEs = mapExamDocs(examEsRepository.findByTitleContaining(kw));
        if (!fromEs.isEmpty()) {
            return Result.ok("success(es)", fromEs);
        }

        List<FriendExam> fromDb = friendExamMapper.selectList(
                Wrappers.<FriendExam>lambdaQuery()
                        .like(FriendExam::getTitle, kw)
                        .orderByDesc(FriendExam::getStartTime)
        );
        syncExamsToEsQuietly(fromDb);
        return Result.ok("success(mysql)", fromDb.stream().map(this::toExamResponse).toList());
    }

    private List<FriendQuestion> mapQuestionDocs(List<QuestionDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<FriendQuestion> list = new ArrayList<>();
        for (QuestionDoc d : docs) {
            if (d == null) {
                continue;
            }
            FriendQuestion q = new FriendQuestion();
            if (StringUtils.hasText(d.getId())) {
                try {
                    q.setQuestionId(Long.parseLong(d.getId().trim()));
                } catch (NumberFormatException ignored) {
                    // ES 脏数据忽略 id
                }
            }
            q.setTitle(d.getTitle());
            q.setContent(d.getContent());
            q.setDifficulty(d.getDifficulty());
            q.setExpectedResult(d.getExpectedResult());
            list.add(q);
        }
        return list;
    }

    private List<ExamResponse> mapExamDocs(List<ExamDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<ExamResponse> list = new ArrayList<>();
        for (ExamDoc d : docs) {
            if (d == null) {
                continue;
            }
            ExamResponse e = new ExamResponse();
            if (StringUtils.hasText(d.getId())) {
                try {
                    e.setExamId(Long.parseLong(d.getId().trim()));
                } catch (NumberFormatException ignored) {
                    // ES 脏数据忽略 id
                }
            }
            e.setTitle(d.getTitle());
            if (d.getStatus() != null) {
                e.setStatus(ExamStatusEnum.labelOf(d.getStatus()));
            }
            list.add(e);
        }
        return list;
    }

    private ExamResponse toExamResponse(FriendExam exam) {
        ExamResponse vo = new ExamResponse();
        vo.setExamId(exam.getExamId());
        vo.setTitle(exam.getTitle());
        vo.setStatus(ExamStatusEnum.labelOf(exam.getStatus()));
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        return vo;
    }

    /**
     * MySQL 回源结果回填 ES（读穿缓存模式）；写 ES 失败不影响主流程返回。
     */
    private void syncQuestionsToEsQuietly(List<FriendQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<QuestionDoc> docs = new ArrayList<>();
        for (FriendQuestion q : questions) {
            if (q == null || q.getQuestionId() == null) {
                continue;
            }
            QuestionDoc doc = new QuestionDoc();
            doc.setId(String.valueOf(q.getQuestionId()));
            doc.setTitle(q.getTitle());
            doc.setContent(q.getContent());
            doc.setDifficulty(q.getDifficulty());
            doc.setDefaultCode(q.getDefaultCode());
            doc.setMainMethod(q.getMainMethod());
            doc.setQuestionCase(q.getQuestionCase());
            doc.setTimeLimit(q.getTimeLimit());
            doc.setSpaceLimit(q.getSpaceLimit());
            doc.setExpectedResult(q.getExpectedResult());
            docs.add(doc);
        }
        if (docs.isEmpty()) {
            return;
        }
        try {
            questionEsRepository.saveAll(docs);
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * MySQL 回源结果回填 ES（读穿缓存模式）；写 ES 失败不影响主流程返回。
     */
    private void syncExamsToEsQuietly(List<FriendExam> exams) {
        if (exams == null || exams.isEmpty()) {
            return;
        }
        List<ExamDoc> docs = new ArrayList<>();
        for (FriendExam e : exams) {
            if (e == null || e.getExamId() == null) {
                continue;
            }
            ExamDoc doc = new ExamDoc();
            doc.setId(String.valueOf(e.getExamId()));
            doc.setTitle(e.getTitle());
            doc.setStatus(e.getStatus());
            doc.setStartTime(e.getStartTime() == null ? null : e.getStartTime().toString());
            doc.setEndTime(e.getEndTime() == null ? null : e.getEndTime().toString());
            docs.add(doc);
        }
        if (docs.isEmpty()) {
            return;
        }
        try {
            examEsRepository.saveAll(docs);
        } catch (Exception ignored) {
            // ignore
        }
    }
}

