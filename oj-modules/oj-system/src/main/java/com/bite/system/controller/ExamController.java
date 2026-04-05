package com.bite.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bite.domain.Result;
import com.bite.system.domain.Exam;
import com.bite.system.domain.dto.ExamAddRequest;
import com.bite.system.service.ExamService;
import org.springframework.web.bind.annotation.*;

/**
 * 竞赛管理接口（tb_exam）。
 */
@RestController
@RequestMapping("/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * 查询所有竞赛（分页，一页 20 条）。
     */
    @GetMapping("/list")
    public Result<IPage<Exam>> list(@RequestParam(value = "page", defaultValue = "1") long page) {
        return examService.list(page);
    }

    @GetMapping("/getExamById")
    public Result<Exam> getExamById(@RequestParam("examId") Long examId) {
        return examService.getExamById(examId);
    }

    @GetMapping("/getExamByName")
    public Result<Exam> getExamByName(@RequestParam("title") String title) {
        return examService.getExamByName(title);
    }

    @PostMapping("/addExam")
    public Result<Long> addExam(@RequestBody ExamAddRequest request) {
        return examService.addExam(request);
    }

    @DeleteMapping("/deleteExamById")
    public Result<Boolean> deleteExamById(@RequestParam("examId") Long examId) {
        return examService.deleteExamById(examId);
    }

    @DeleteMapping("/deleteExamByName")
    public Result<Integer> deleteExamByName(@RequestParam("title") String title) {
        return examService.deleteExamByName(title);
    }
}

