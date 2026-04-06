package com.bite.friend.service.impl;

import com.bite.common.elasticsearch.question.doc.QuestionDoc;
import com.bite.common.elasticsearch.question.repo.QuestionEsRepository;
import com.bite.friend.domain.FriendQuestion;
import com.bite.friend.mapper.FriendQuestionMapper;
import com.bite.friend.service.FriendQuestionForJudgeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class FriendQuestionForJudgeServiceImpl implements FriendQuestionForJudgeService {

    private final QuestionEsRepository questionEsRepository;
    private final FriendQuestionMapper friendQuestionMapper;

    public FriendQuestionForJudgeServiceImpl(QuestionEsRepository questionEsRepository,
                                            FriendQuestionMapper friendQuestionMapper) {
        this.questionEsRepository = questionEsRepository;
        this.friendQuestionMapper = friendQuestionMapper;
    }

    @Override
    public FriendQuestion loadQuestion(Long questionId) {
        if (questionId == null) {
            return null;
        }
        Optional<QuestionDoc> hit = questionEsRepository.findById(String.valueOf(questionId));
        if (hit.isPresent()) {
            return fromDoc(hit.get());
        }
        FriendQuestion q = friendQuestionMapper.selectById(questionId);
        if (q != null) {
            syncOneToEsQuietly(q);
        }
        return q;
    }

    private static FriendQuestion fromDoc(QuestionDoc d) {
        FriendQuestion q = new FriendQuestion();
        if (d != null && StringUtils.hasText(d.getId())) {
            try {
                q.setQuestionId(Long.parseLong(d.getId().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (d == null) {
            return q;
        }
        q.setTitle(d.getTitle());
        q.setContent(d.getContent());
        q.setDifficulty(d.getDifficulty());
        q.setDefaultCode(d.getDefaultCode());
        q.setMainMethod(d.getMainMethod());
        q.setQuestionCase(d.getQuestionCase());
        q.setTimeLimit(d.getTimeLimit());
        q.setSpaceLimit(d.getSpaceLimit());
        q.setExpectedResult(d.getExpectedResult());
        return q;
    }

    private void syncOneToEsQuietly(FriendQuestion q) {
        if (q == null || q.getQuestionId() == null) {
            return;
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
        try {
            questionEsRepository.saveAll(List.of(doc));
        } catch (Exception ignored) {
        }
    }
}
