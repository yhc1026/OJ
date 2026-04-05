package com.bite.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bite.domain.Result;
import com.bite.system.domain.Exam;
import com.bite.system.domain.dto.ExamAddRequest;

/**
 * 考试（tb_exam）业务接口。
 */
public interface ExamService extends IService<Exam> {

    Result<IPage<Exam>> list(long page);

    Result<Exam> getExamById(Long examId);

    Result<Exam> getExamByName(String title);

    Result<Long> addExam(ExamAddRequest request);

    Result<Boolean> deleteExamById(Long examId);

    Result<Integer> deleteExamByName(String title);
}

