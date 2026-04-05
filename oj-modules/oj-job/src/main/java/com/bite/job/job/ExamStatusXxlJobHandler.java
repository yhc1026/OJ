package com.bite.job.job;

import com.bite.job.service.ExamStatusRefreshService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 竞赛状态刷新：调度中心 JobHandler 名称为 {@code examStatusRefreshHandler}。
 */
@Component
public class ExamStatusXxlJobHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamStatusXxlJobHandler.class);

    private final ExamStatusRefreshService examStatusRefreshService;

    public ExamStatusXxlJobHandler(ExamStatusRefreshService examStatusRefreshService) {
        this.examStatusRefreshService = examStatusRefreshService;
    }

    @XxlJob("examStatusRefreshHandler")
    public void examStatusRefreshHandler() {
        try {
            examStatusRefreshService.refreshByTime();
            XxlJobHelper.handleSuccess();
        } catch (Exception e) {
            log.error("examStatusRefreshHandler failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
}
