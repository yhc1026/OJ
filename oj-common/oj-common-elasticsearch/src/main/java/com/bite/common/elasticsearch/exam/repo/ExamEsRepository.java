package com.bite.common.elasticsearch.exam.repo;

import com.bite.common.elasticsearch.exam.doc.ExamDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 竞赛检索 Repository（公共组件）。
 */
public interface ExamEsRepository extends ElasticsearchRepository<ExamDoc, String> {

    /** 竞赛 id 模糊匹配（基于 ES 文档 id 字符串） */
    List<ExamDoc> findByIdContaining(String keyword);

    /** 题目名称模糊匹配 */
    List<ExamDoc> findByTitleContaining(String keyword);
}

