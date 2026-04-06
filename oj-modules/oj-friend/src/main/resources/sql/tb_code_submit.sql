-- 代码提交与判题结果表（与 FriendCodeSubmit 对齐）
CREATE TABLE IF NOT EXISTS tb_user_submit (
  submit_id   BIGINT       NOT NULL COMMENT '提交id',
  user_id       BIGINT       NOT NULL,
  question_id   BIGINT       NOT NULL,
  exam_id       BIGINT                DEFAULT NULL COMMENT '考试id，可为空',
  user_code     TEXT         NOT NULL,
  language      TINYINT      NOT NULL COMMENT '0=Java',
  exe_message   VARCHAR(1024)         DEFAULT NULL,
  score         INT          NOT NULL DEFAULT 0,
  create_by     BIGINT       NOT NULL,
  create_time   DATETIME     NOT NULL,
  update_by     BIGINT                DEFAULT NULL,
  update_time   DATETIME              DEFAULT NULL,
  status        TINYINT      NOT NULL COMMENT '0通过 1失败 2中间态',
  PRIMARY KEY (submit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
