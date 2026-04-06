package com.bite.system;

import com.bite.common.elasticsearch.question.doc.QuestionDoc;
import com.bite.common.elasticsearch.question.repo.QuestionEsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Elasticsearch 测试类
 */
@SpringBootTest
public class ElasticsearchTest {

    @Autowired(required = false)
    private QuestionEsRepository questionEsRepository;

    @Test
    public void testFindAllQuestions() {
        if (questionEsRepository == null) {
            System.out.println("QuestionEsRepository 未注入，请检查 ES 配置");
            return;
        }

        List<QuestionDoc> all = questionEsRepository.findAll();
        System.out.println("===== ES 中共有 " + all.size() + " 条题目 =====");

        for (QuestionDoc doc : all) {
            System.out.println("----------------------------------------");
            System.out.println("id: " + doc.getId());
            System.out.println("title: " + doc.getTitle());
            System.out.println("content: " + doc.getContent());
            System.out.println("difficulty: " + doc.getDifficulty());
            System.out.println("defaultCode: " + doc.getDefaultCode());
            System.out.println("mainMethod: " + doc.getMainMethod());
            System.out.println("questionCase: " + doc.getQuestionCase());
            System.out.println("timeLimit: " + doc.getTimeLimit());
            System.out.println("spaceLimit: " + doc.getSpaceLimit());
            System.out.println("expectedResult: " + doc.getExpectedResult());
        }
    }
}
