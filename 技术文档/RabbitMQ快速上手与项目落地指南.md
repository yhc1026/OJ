# RabbitMQ快速上手与项目落地指南

## 1. 文档目标

这份文档用于帮助你快速掌握 RabbitMQ，并理解它在当前 OJ 项目中的完整落地方案。读完后你应当可以：

- 独立启动 RabbitMQ 并完成基础验证
- 看懂消息从 `oj-friend` 到 `oj-judge` 的完整链路
- 理解 ACK、死信、幂等、重试等关键机制
- 在本项目中定位问题并完成排障
- 具备上线前的配置与容量评估思路

---

## 2. RabbitMQ 是什么，为什么要用

RabbitMQ 是一个消息中间件。你可以把它理解为“可靠的任务邮局”：

- 生产者（Producer）把任务交给邮局
- 邮局先保存任务再转发
- 消费者（Consumer）按节奏取任务处理

在本项目里：

- `oj-friend` 负责接收前端提交（生产者）
- `oj-judge` 负责执行判题（消费者）
- 两者通过 RabbitMQ 解耦

### 2.1 引入 MQ 的收益

- **解耦**：`friend` 不必同步等待 `judge` 返回
- **削峰**：高峰提交时先入队，后端平滑消费
- **可靠性**：消费者故障不会立即丢任务
- **可扩展**：后续可横向扩容多个 `judge` 消费实例

---

## 3. RabbitMQ 核心概念（项目实战版）

### 3.1 Producer（生产者）

发送消息的一方。  
本项目：`oj-friend` 在提交代码后发送 `JudgeRunTaskMessage`。

### 3.2 Consumer（消费者）

接收并处理消息的一方。  
本项目：`oj-judge` 监听队列，取出任务并执行判题。

### 3.3 Queue（队列）

真正保存消息的地方。  
本项目主队列：`oj.judge.queue`。

### 3.4 Exchange（交换机）

负责接收消息并路由到队列。  
本项目使用 Direct 类型交换机：`oj.judge.exchange`。

### 3.5 Routing Key（路由键）

Direct 交换机按 routingKey 精确匹配。  
本项目路由键：`oj.judge.run`。

### 3.6 ACK / NACK

- **ACK**：消费者处理成功，告诉 RabbitMQ 可删除消息
- **NACK**：处理失败，按策略重回队列或进死信队列

你当前实现是手动 ACK：处理完成再 ACK，可靠性更高。

### 3.7 Dead Letter（死信）

失败消息进入死信交换机/死信队列，方便后续排查与补偿。  
本项目死信：

- `oj.judge.dlx.exchange`
- `oj.judge.dlx.queue`
- `oj.judge.run.dlx`

---

## 4. 本项目 MQ 架构设计

## 4.1 组件关系

- 生产者：`oj-friend`
- 消费者：`oj-judge`
- 公共模块：`oj-common-MQ`（统一交换机、队列、消息体）

### 4.2 资源命名

- 交换机：`oj.judge.exchange`（Direct, durable）
- 队列：`oj.judge.queue`（durable）
- 路由键：`oj.judge.run`
- 死信交换机：`oj.judge.dlx.exchange`（Direct, durable）
- 死信队列：`oj.judge.dlx.queue`（durable）
- 死信路由键：`oj.judge.run.dlx`

### 4.3 消息体

消息体：`JudgeRunTaskMessage`，核心字段包括：

- `messageId`：消息唯一 ID（幂等关键）
- `submitId`：提交 ID（回写结果关键）
- `userId/questionId/examId`
- `userCode/mainMethod/testInput/expectedOutput`
- `timeLimitMs/spaceLimitKb/language`

---

## 5. 端到端时序（你项目当前实现）

1. 前端调用 `POST /friend/judge/submit`
2. `oj-friend` 校验参数 + 鉴权 + 读取题目配置
3. `oj-friend` 先写 `tb_user_submit`，状态 `status=2(PENDING)`
4. `oj-friend` 构造 `JudgeRunTaskMessage` 并发送到 `oj.judge.exchange`
5. RabbitMQ 的直连交换机通过查映射表，将消息自带的路由键和队列自带的绑定键进行完全匹配，把消息路由到 `oj.judge.queue`
6. `oj-judge` 消费消息，先写幂等日志（防重复，messageId，status）
7. `oj-judge` 执行判题逻辑（编译、运行、比对）
8. 判题完成后judge-service自主更新 `tb_user_submit` 为 `status=0/1`
9. 消费成功则手动向队列 `ACK`，队列丢弃消息
10. 若异常则手动向队列发送 `NACK` ，队列把消息放入死信队列。进入死信队列的消息，由管理员手动通过get/publish message重新入队列

---

## 6. 本地快速启动 RabbitMQ

## 6.1 Docker 启动

```bash
docker run -d --name rabbitmq ^
  -p 5672:5672 -p 15672:15672 ^
  rabbitmq:3-management
```

说明：

- `5672`：应用连接端口（AMQP）
- `15672`：管理台端口（Web UI）

### 6.2 管理台登录

- 地址：`http://localhost:15672`
- 默认用户名：`guest`
- 默认密码：`guest`

---

## 7. 项目配置说明（非常关键）

## 7.1 friend/ judge 都需要配置连接

你项目已在 `bootstrap.yml` 支持：

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VHOST`

### 7.2 Windows PowerShell 临时设置示例

```powershell
$env:RABBITMQ_HOST="127.0.0.1"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="admin"
$env:RABBITMQ_PASSWORD="123456"
$env:RABBITMQ_VHOST="/"
```

设置后在同一终端启动服务即可生效。

### 7.3 judge 端监听参数

当前配置要点：

- `acknowledge-mode: manual`：手动 ACK
- `prefetch: 1`：每次拉取 1 条，避免单实例堆积过多未确认消息

---

## 8. ACK / NACK 机制详解

### 8.1 为什么一定要手动 ACK

如果自动 ACK，消息一到消费者就会被认为“成功”，即便业务处理中途异常，消息也可能丢失。  
手动 ACK 的语义是：**业务成功 -> 再确认删除消息**。

### 8.2 你项目当前处理策略

- 业务成功：`basicAck`（删除消息）
- 业务失败：`basicNack(..., requeue=false)`（不回主队列，进死信）

这种策略的优点：

- 主队列不会被坏消息反复“毒化”
- 问题消息统一进入 DLQ，便于运维处理

---

## 9. 幂等机制详解（防重复消费）

RabbitMQ 在“至少一次投递”模型下，重复消费是正常现象（网络抖动、重连、重投等）。  
所以消费者必须幂等。

你项目实现：

- 表：`tb_judge_mq_consume_log`
- 主键：`message_id`
- 消费时先 `insertIfAbsent`
  - 插入成功：第一次消费，继续判题
  - 插入失败：重复消息，直接 ACK 丢弃

这套方案简单有效，建议保留。

---

## 10. MQ消息、配置持久化机制

需要保证1. 生产者-MQ的消息安全；2. MQ内部配置的安全；3. MQ内部消息的安全；4. MQ-消费者的消息安全。具体配置如下：

| 阶段 | 核心风险 | 保证机制 | 状态/备注 |
| :--- | :--- | :--- | :--- |
| **1. 生产者 → MQ** | 消息根本没发到MQ | `Publisher Confirm` | **已满足** ✅ |
| **2. MQ内部配置** | MQ重启后队列/交换机消失 | `durable=true` | **已满足** ✅ |
| **3. MQ内部消息** | MQ重启后内存中的消息丢失 | 消息 `PERSISTENT` 模式 | **已满足** ✅ |
| **4. MQ → 消费者** | 消息投递后，消费者处理失败 | `手动ACK` + `业务幂等` | **已满足** ✅ |

---

## 11. 数据库表说明

### 11.1 判题主表（不记录消息队列的判题状态）

`tb_user_submit`：保存提交记录和最终判题结果。

### 11.2 MQ 幂等日志表（记录消息队列的判题状态）

| messageId | commitId | status | err-msg |
| :--- | :--- | :--- | :--- |

---

## 12. 前端如何拿到判题结果

推荐方式：轮询（当前已实现）。

### 12.1 接口

- 提交：`POST /friend/judge/submit`
- 查结果：`GET /friend/judge/result/{submitId}`

### 12.2 前端轮询策略

- 提交成功后拿 `submitId`
- 每 1~2 秒查询一次结果
- `status=2`：继续轮询
- `status=0/1`：停止轮询并展示结果
- 建议设置总超时时间（例如 60 秒）

---

## 13. 改进建议

### 13.1 容量与并发：应对消息积压

- 增加 `oj-judge` 实例数提升吞吐
- 根据机器性能合理调整 prefetch

### 13.2 队列无上限的风险与改进

- 通过配置，设置静态参数，比如队列最多容纳10w条数据，比如message-length（队列长度）、message-ttl（队列过期时间）等

### 13.3 可靠性与观测

- 打开消息发送确认（publisher confirm，可后续增强）
- 给消息增加 `timestamp` 与业务 traceId
- 监控以下指标：
  - 队列堆积数
  - 消费速率
  - 死信速率
  - 判题耗时分位数（P95/P99）

---

## 14. 常见问题与排障手册

### 14.1 发送了消息但 judge 不消费

排查顺序：

1. `oj-judge` 是否启动成功
2. RabbitMQ 账号密码/vhost 是否正确
3. 管理台队列 `oj.judge.queue` 是否存在
4. 队列是否有 `Ready` 消息积压
5. `oj-judge` 日志是否有监听异常

### 14.2 消息一直进死信队列

常见原因：

- 业务异常（判题流程抛错）
- 数据缺失（`submitId/messageId` 为空）
- 数据库更新失败

操作建议：

- 先查 `tb_judge_mq_consume_log.last_error`
- 再看 `oj-judge` 日志堆栈
- 修复后可做“死信重投”脚本

### 14.3 前端一直看到 status=2

可能原因：

- judge 未消费
- 消费后回写 `tb_user_submit` 失败

优先检查：

- RabbitMQ 队列堆积
- `tb_judge_mq_consume_log` 消费状态
- judge 端数据库连接