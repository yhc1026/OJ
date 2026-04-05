package com.bite.common.elasticsearch.question.repo;

import com.bite.common.elasticsearch.question.doc.QuestionDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目检索 Repository（公共组件）。
 */
public interface QuestionEsRepository extends ElasticsearchRepository<QuestionDoc, String> {

    /** 题目 id 模糊匹配（基于 ES 文档 id 字符串） */
    List<QuestionDoc> findByIdContaining(String keyword);

    /** 标题包含关键字（Spring Data 自动生成查询） */
    List<QuestionDoc> findByTitleContaining(String keyword);

    /** 按难度精确匹配（0/1/2）。 */
    List<QuestionDoc> findByDifficulty(Integer difficulty);
}

