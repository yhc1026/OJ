package com.bite.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bite.judge.domain.JudgeMqConsumeLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JudgeMqConsumeLogMapper extends BaseMapper<JudgeMqConsumeLog> {

    @Insert("INSERT INTO tb_judge_mq_consume_log(message_id, submit_id, consume_status, create_time, update_time) " +
            "VALUES(#{messageId}, #{submitId}, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE message_id = message_id")
    int insertIfAbsent(@Param("messageId") String messageId, @Param("submitId") Long submitId);
}
