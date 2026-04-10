package com.bite.judge.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.mq.constants.JudgeMqConstants;
import com.bite.common.mq.message.JudgeRunTaskMessage;
import com.bite.domain.Result;
import com.bite.judge.domain.JudgeCodeSubmit;
import com.bite.judge.domain.JudgeMqConsumeLog;
import com.bite.judge.domain.dto.friend.JudgeRunRequest;
import com.bite.judge.domain.dto.friend.JudgeSingleCaseResponse;
import com.bite.judge.mapper.JudgeCodeSubmitMapper;
import com.bite.judge.mapper.JudgeMqConsumeLogMapper;
import com.bite.judge.service.JudgeRunService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JudgeTaskConsumer {

    private static final int STATUS_PASS = 0;
    private static final int STATUS_FAIL = 1;
    private static final int STATUS_PENDING = 2;
    private static final int EXE_MSG_MAX = 1024;

    private final JudgeRunService judgeRunService;
    private final JudgeCodeSubmitMapper judgeCodeSubmitMapper;
    private final JudgeMqConsumeLogMapper judgeMqConsumeLogMapper;

    public JudgeTaskConsumer(JudgeRunService judgeRunService,
                             JudgeCodeSubmitMapper judgeCodeSubmitMapper,
                             JudgeMqConsumeLogMapper judgeMqConsumeLogMapper) {
        this.judgeRunService = judgeRunService;
        this.judgeCodeSubmitMapper = judgeCodeSubmitMapper;
        this.judgeMqConsumeLogMapper = judgeMqConsumeLogMapper;
    }

    @RabbitListener(queues = JudgeMqConstants.JUDGE_QUEUE)
    public void consume(JudgeRunTaskMessage task, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (task == null || task.getSubmitId() == null || task.getMessageId() == null) {
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        int inserted = judgeMqConsumeLogMapper.insertIfAbsent(task.getMessageId(), task.getSubmitId());
        if (inserted == 0) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            JudgeRunRequest request = buildRunRequest(task);
            Result<JudgeSingleCaseResponse> result = judgeRunService.run(request);

            int status = STATUS_FAIL;
            int score = 0;
            String exeMsg = "判题失败";
            if (result != null && result.getCode() == ResultCode.SUCCESS.getCode() && result.getData() != null) {
                JudgeSingleCaseResponse data = result.getData();
                score = data.getVerdict() == 0 ? 1 : 0;
                status = score == 1 ? STATUS_PASS : STATUS_FAIL;
                exeMsg = trunc(data.getMessage(), EXE_MSG_MAX);
            } else if (result != null) {
                exeMsg = trunc(result.getMsg(), EXE_MSG_MAX);
            }

            updateSubmitResult(task.getSubmitId(), task.getUserId(), status, score, exeMsg);
            updateConsumeLog(task.getMessageId(), 1, null);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            updateSubmitResult(task.getSubmitId(), task.getUserId(), STATUS_FAIL, 0, trunc(ex.getMessage(), EXE_MSG_MAX));
            updateConsumeLog(task.getMessageId(), 2, trunc(ex.getMessage(), 500));
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private JudgeRunRequest buildRunRequest(JudgeRunTaskMessage task) {
        JudgeRunRequest request = new JudgeRunRequest();
        request.setUserCode(task.getUserCode());
        request.setMainMethod(task.getMainMethod());
        request.setTestInput(task.getTestInput());
        request.setExpectedOutput(task.getExpectedOutput());
        request.setTimeLimitMs(task.getTimeLimitMs());
        request.setSpaceLimitKb(task.getSpaceLimitKb());
        request.setLanguage(task.getLanguage());
        return request;
    }

    private void updateSubmitResult(Long submitId, Long userId, int status, int score, String exeMsg) {
        JudgeCodeSubmit update = new JudgeCodeSubmit();
        update.setStatus(status);
        update.setScore(score);
        update.setExeMessage(exeMsg);
        update.setUpdateBy(userId);
        update.setUpdateTime(LocalDateTime.now());
        judgeCodeSubmitMapper.update(update, new LambdaUpdateWrapper<JudgeCodeSubmit>()
                .eq(JudgeCodeSubmit::getSubmitId, submitId)
                .eq(JudgeCodeSubmit::getStatus, STATUS_PENDING));
    }

    private void updateConsumeLog(String messageId, int consumeStatus, String error) {
        JudgeMqConsumeLog update = new JudgeMqConsumeLog();
        update.setConsumeStatus(consumeStatus);
        update.setLastError(error);
        update.setUpdateTime(LocalDateTime.now());
        judgeMqConsumeLogMapper.update(update, new LambdaUpdateWrapper<JudgeMqConsumeLog>()
                .eq(JudgeMqConsumeLog::getMessageId, messageId));
    }

    private String trunc(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
