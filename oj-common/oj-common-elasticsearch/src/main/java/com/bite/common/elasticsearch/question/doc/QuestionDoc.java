package com.bite.common.elasticsearch.question.doc;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 题目检索文档（Elasticsearch 公共组件）。
 */
@Document(indexName = "oj_question")
public class QuestionDoc {

    /** ES 文档 id：与 tb_question.question_id 对齐（字符串避免大整数精度问题） */
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Integer)
    private Integer difficulty;

    @Field(type = FieldType.Text)
    private String defaultCode;

    @Field(type = FieldType.Text)
    private String mainMethod;

    @Field(type = FieldType.Text)
    private String questionCase;

    @Field(type = FieldType.Long)
    private Long timeLimit;

    @Field(type = FieldType.Long)
    private Long spaceLimit;

    @Field(type = FieldType.Text)
    private String expectedResult;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getDefaultCode() {
        return defaultCode;
    }

    public void setDefaultCode(String defaultCode) {
        this.defaultCode = defaultCode;
    }

    public String getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(String mainMethod) {
        this.mainMethod = mainMethod;
    }

    public String getQuestionCase() {
        return questionCase;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public void setQuestionCase(String questionCase) {
        this.questionCase = questionCase;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Long getSpaceLimit() {
        return spaceLimit;
    }

    public void setSpaceLimit(Long spaceLimit) {
        this.spaceLimit = spaceLimit;
    }
}

