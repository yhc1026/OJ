# Prometheus 与 Grafana 监控指南（本项目落地版）

## 1. 文档目标

阅读本文后，你应能说明 Prometheus / Grafana 各自做什么、底层如何协作；能在本仓库完成监控栈的部署与验证；能在 Grafana 中用「低代码」方式配置大盘与查询；并覆盖常见面试考点。

---

## 2. 两个组件分别做什么（功能）

### 2.1 Prometheus

- **采集与存储**：按固定时间间隔从各目标的 **HTTP 端点**（本项目为 `/actuator/prometheus`）**拉取（pull）** 指标文本，解析后写入本地 **时序数据库（TSDB）**。
- **查询**：内置 **PromQL** 语言，支持聚合、速率、分位数等计算。
- **告警（可选）**：可配置 **告警规则**，对接 **Alertmanager** 做路由、静默、通知（邮件、钉钉、Webhook 等）。本项目默认 compose **未** 内置 Alertmanager，可按需扩展。

### 2.2 Grafana

- **可视化**：连接 Prometheus（或其它数据源），把时序数据画成 **折线图、柱状图、表格** 等，组成 **Dashboard**。
- **低代码体验**：通过 **Explore**、**Panel 编辑器**、变量、模板等，无需写后端代码即可配置查询与图表。
- **告警（可选）**：Grafana **Alerting** 可基于查询结果配置通知渠道，与 Prometheus 告警二选一或组合使用。

### 2.3 埋点的作用，以及如何与 Prometheus 配合

**埋点是什么**  
在关键业务路径（如登录、判题 compile/run、XXL 任务执行）上，用代码 **记录可观测信号**：次数（Counter）、耗时（Timer/Histogram）等。没有埋点，Prometheus 只能看到 JVM、HTTP 等「通用」指标，**看不到业务语义**（例如「每分钟登录多少次」）。
埋点可以类比成管道中的流速计，默默记录管道的流速，只有人需要读的时候才显示

**埋点的作用**

- **量化业务**：把「发生了什么、发生多少次、耗时多少」变成时间序列，便于大盘与 SLA。
- **排障与归因**：出问题时结合 **指标 + 日志 + 链路** 缩小范围（例如判题 `phase=run` 耗时突增）。
- **告警与容量**：在 PromQL / Grafana 告警里对埋点指标设阈值（如错误率、QPS 异常）。

**如何与 Prometheus 配合（本项目链路）**

1. **应用内**：Spring 使用 **Micrometer** 注册指标（如 `user.actions`、`judge.operation`）；**Actuator + Prometheus Registry** 把它们序列化成 **Prometheus 文本格式**。
2. **暴露**：进程对外提供 **`GET /actuator/prometheus`**，返回当前时刻所有已注册指标的「快照」文本。
3. **采集**：Prometheus 按 **`scrape_interval`** **HTTP 拉取**该端点，把样本写入 **TSDB**（不是应用推给 Prometheus）。
4. **使用**：在 Prometheus 或 Grafana 中用 **PromQL** 查询、聚合；**Grafana 不负责采集**，只读 Prometheus 中的数据。

**小结**：埋点解决「**业务数据从哪来**」；Prometheus 解决「**存起来、可查、可告警**」；二者通过 **同一套暴露端点 + 拉取协议** 衔接，无需在业务代码里写 PromQL。

**分工一句话（承接 2.1～2.3）**：应用通过 **埋点 + Actuator** **暴露指标**；Prometheus **拉取并存数**；Grafana **看图与告警配置界面**。

---

## 3. 原理简述（如何串起来）

### 3.1 数据流

```
各 Spring Boot 进程
  └─ Actuator + Micrometer
       └─ 对外 HTTP：GET /actuator/prometheus  （Prometheus 文本格式）

Prometheus Server
  └─ 按 scrape_interval 访问各 target
  └─ 本地 TSDB 持久化

Grafana
  └─ Data Source 指向 http://prometheus:9090（或宿主机端口）
  └─ Panel 中写 PromQL → 展示曲线
```

### 3.2 Actuator、Micrometer、MeterRegistry 是什么（概念与类比）

三者都在 **应用进程内** 协作，**不负责** 把数据送到 Prometheus（那是 Prometheus **拉取** `/actuator/prometheus` 的事）。

**一句话类比（帮助记忆）**

| 概念 | 可类比为 | 实际职责 | 归属 |
|------|----------|----------|------|
| **Spring Boot Actuator** | **发布日志的员工** | 指标读取者、接口暴露者：提供 **HTTP 端点**（如 `/actuator/health`、`/actuator/prometheus`），把 **已登记在注册表里的指标** 按约定格式 **对外输出**；也包含健康检查、环境信息等。可以理解为：**把程序员在代码里通过 Micrometer 登记好的埋点结果，统一暴露给外部采集**（再配合 `management.endpoints.web.exposure.include` 控制开放哪些端点）。 | **Spring Boot** |
| **Micrometer** | **测量工具供应商** | 测量工具供应商：**与监控系统无关的抽象 API**：`Counter`、`Timer`、`Gauge` 等；业务或框架用同一套 API 记录数值，**导出成 Prometheus 格式** 时由 **`micrometer-registry-prometheus`** 适配。 | **Micrometer (独立开源项目)** |
| **MeterRegistry** | **存放日志的仓库** | 存放各项指标的仓库：进程内 **单例式的注册中心**：`Counter.builder(...).register(registry)`、`Timer.builder(...).register(registry)` 都把 **Meter** 放进 **MeterRegistry**；Actuator 暴露 Prometheus 端点时，本质是从这个 **日志** 里 **读出当前所有指标** 再序列化。 | **Micrometer** |

**三者关系（调用顺序）**

1. 代码里拿到 **`MeterRegistry`**（通常 Spring 注入）。
2. 用 **Micrometer** 的 API 创建或复用 **Counter / Timer** 等，并 **register 到 MeterRegistry**（AOP、业务代码、Spring 自动配置都会往这里登记）。
3. **Actuator** 的 **`/actuator/prometheus`** 端点读取 **MeterRegistry** 中的内容，输出 **Prometheus 文本**。

因此：**Actuator是接口暴露者，把监测的接口对外暴露；Prometheus是接口请求者，专门请求接口，读取监控数据；Micrometer是检测指标供应商，负责提供counter、timer等检测指标；MeterRegistry收集监控指标，Actuator读取MeterRegistry收集的数据对外暴露**。

### 3.3 Pull 模型

- Prometheus **主动抓取**，应用 **不主动推送**（特殊场景可用 Pushgateway，本项目未使用）。
- 抓取失败会在 **Status → Targets** 中显示 **DOWN**，需检查网络、端口、路径、防火墙。

### 3.4 指标类型（与 PromQL 的关系）

| 类型 | 含义 | PromQL 注意点 |
|------|------|----------------|
| Counter | 只增不减的计数 | 看「每秒增量」用 `rate()` / `increase()`，**范围窗口建议 ≥ 2× scrape 间隔**（如 15s 抓取则用 `[1m]`～`[5m]`） |
| Gauge | 可增可减的瞬间值 | 可直接 `avg_over_time` 等 |
| Histogram(Timer) / Summary | 分布与分位数 | Timer 在 Prometheus 中常对应 `_count`、`_sum`、`_bucket` 等 |

### 3.5 本项目的指标从哪来

- **Spring Boot Actuator + Micrometer Prometheus Registry**：**自动暴露** JVM、Tomcat/WebFlux、数据源等 **默认指标**。
- **`oj-common-AOP` 埋点**：业务 Counter / Timer（见第 6 节）。

### 3.6 如何通过 AOP 把埋点「织入」业务

这里说的 **织入**，指：**不改业务方法内部代码**，在 **调用链外层** 自动加上（记一次 Counter / 记一段 Timer）的逻辑。本项目用 **Spring AOP**（`@Aspect` + `@Around`），在 **容器启动后** 对符合条件的 Bean 生成代理，外部每次调用目标方法都会先经过切面。

**织入在本仓库里按什么顺序生效**

1. **引入模块**：业务服务依赖 **`oj-common-AOP`**，并具备 **Actuator + Micrometer**（提供 `MeterRegistry`）。
2. **自动配置**：`META-INF/spring/...AutoConfiguration.imports` 注册 **`OjMetricsAutoConfiguration`**；其中 **`@AutoConfigureAfter(MetricsAutoConfiguration.class)`** 保证 **`MeterRegistry` 已就绪** 再创建切面 Bean。
3. **按类路径注册切面**：通过 **`@ConditionalOnClass`** 判断当前进程里是否存在对应类（如 `SysUserController`、`RunAndOutput`、`@XxlJob`），只注册 **本服务需要** 的切面，避免无关模块报错。
4. **切点匹配**：切面里用 **execution / @annotation** 等写出 **切点表达式**，指向具体包下的 **Controller 方法**、`RunAndOutput.compile/run`、或 **带 `@XxlJob` 的方法**。
5. **一次请求/调用的执行顺序**：外部调用进入 **代理** → 切面 **`@Around`** 先执行（`Counter.increment()` 或 `Timer` 采样）→ **`ProceedingJoinPoint.proceed()`** 调 **原业务方法** → 返回后再结束计时（若为 Timer）。业务类里 **不出现** Micrometer 代码。
6. **与 Prometheus 的衔接**：AOP 只负责把数据写入 **Micrometer**；同进程内的 **Actuator** 将指标暴露在 **`/actuator/prometheus`**；**Prometheus 拉取** 与 AOP **无直接耦合**。

**注意**：**`oj-gateway`（WebFlux）** 不走 Servlet Controller 这一套切点，当前 **未** 用该 AOP 织入；若要对网关做 HTTP 类指标，需在 **Filter** 里自行调 `MeterRegistry`（见 §5.6、§6.2）。

---

## 4. 面试高频知识点

1. **Prometheus 是 pull 还是 push？** 默认 **pull**；短时任务可用 Pushgateway 补 push 场景。
2. **PromQL 里 `rate()` 和 `irate()` 区别？** `rate` 更平滑，适合告警与大盘；`irate` 只看相邻两点，波动大。
3. **为什么 `increase(x[5s])` 经常没数据？** 抓取间隔通常 10s～30s，窗口小于间隔或样本不足会导致空序列；一般用 `[1m]`～`[5m]`。
4. **Counter 为什么导出带 `_total`？** OpenMetrics / Prometheus 约定，Micrometer 对 Counter 会映射为 `xxx_total`。
5. **Grafana 与 Prometheus 分工？** Prometheus 存储与查询引擎；Grafana 可视化与告警 UI，不替代 TSDB。
6. **服务发现？** 静态 `static_configs`、Kubernetes SD、Consul 等；本项目 **开发环境** 使用 **静态 IP:端口**（`host.docker.internal`）。
7. **RED / USE 方法论？** Rate、Errors、Duration；Utilization、Saturation、Errors——面试可结合 JVM 与 HTTP 指标举例。

---

## 5. 在本项目中的配置与部署

### 5.1 代码侧（各微服务）

- 已在 **`oj-gateway`**、**`oj-modules` 下各业务服务** 中引入：
  - `spring-boot-starter-actuator`
  - `micrometer-registry-prometheus`
- 各服务 `application.yml`（或等价配置）中暴露：`health,info,prometheus`。
- 业务埋点模块：**`oj-common-AOP`**（自动配置类：`OjMetricsAutoConfiguration`），由 **`oj-system`、`oj-friend`、`oj-judge`、`oj-job`** 引入依赖。

### 5.2 监控栈目录

| 路径 | 说明 |
|------|------|
| `oj-prometheus/docker-compose.yml` | 启动 Prometheus + Grafana 容器 |
| `oj-prometheus/prometheus/prometheus.yml` | 抓取任务、间隔、目标地址 |
| `oj-prometheus/grafana/provisioning/datasources/prometheus.yml` | Grafana 预置 Prometheus 数据源 |

### 5.3 部署步骤（Docker）

1. 本机先启动依赖：**Nacos**、**Redis**、**MySQL** 等，以及需要观察的 **Java 微服务**（见下表端口）。
2. 进入目录：`oj-prometheus`
3. 执行：`docker compose up -d`
4. 浏览器访问：
   - Prometheus：`http://localhost:9090`
   - Grafana：`http://localhost:3000`（默认账号 **admin / admin**，首次登录可改密码）

### 5.4 宿主机与容器网络

- Prometheus / Grafana 跑在 **Docker 内**，业务进程跑在 **宿主机** 时，抓取地址使用 **`host.docker.internal:端口`**（Windows / Docker Desktop 常见；`docker-compose` 中已配置 `extra_hosts` 便于 Linux 兼容）。
- 若业务也改为 **全容器** 且与 Prometheus 同网络，应把 `targets` 改为 **容器服务名:端口**，而不是 `host.docker.internal`。

### 5.5 端口一览（与 `prometheus.yml` 一致）

| 服务 | 默认端口 | 指标路径 |
|------|----------|----------|
| oj-gateway | 9000 | `/actuator/prometheus` |
| oj-system | 9201 | 同上 |
| oj-judge | 9202 | 同上 |
| oj-job | 9203 | 同上 |
| oj-friend | 9204 | 同上 |

### 5.6 业务埋点如何集成到项目中（`oj-common-AOP`）

**1. Maven 依赖**  
在需要业务埋点的微服务 `pom.xml` 中增加（版本与工程一致）：

```xml
<dependency>
  <groupId>com.bite</groupId>
  <artifactId>oj-common-AOP</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

当前已引入的服务：**`oj-system`、`oj-friend`、`oj-judge`、`oj-job`**。父工程 **`oj-common`** 已聚合模块 **`oj-common-AOP`**，需先 **`mvn install`** 公共模块后再启动业务服务。

**2. 前提：Actuator + Prometheus**  
同一服务必须已依赖 **`spring-boot-starter-actuator`** 与 **`micrometer-registry-prometheus`**，并暴露 **`prometheus`** 端点（见 §5.1）。`MeterRegistry` 由 Actuator 创建；`OjMetricsAutoConfiguration` 使用 **`@AutoConfigureAfter(MetricsAutoConfiguration.class)`**，避免切面注册早于 `MeterRegistry` 导致埋点不生效。

**3. 自动装配（无需 `@Import`）**  
类路径下存在文件：

`oj-common-AOP/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

内容为 `com.bite.common.aop.config.OjMetricsAutoConfiguration`。Spring Boot 启动时会加载该自动配置，按 **`@ConditionalOnClass`** 只注册 **当前服务存在的** 切面 Bean（例如仅有 `oj-system` 时不会强加载 Friend 的 Controller 切面）。

**4. 切面里做了什么**  
- **登录/注册**：`@Around` 切 `SysUserController` / `UserOperationController` 指定方法，**Counter** 自增。  
- **判题**：切 `RunAndOutput.compile` / `run`，**Timer** 记录耗时与次数。  
- **XXL-JOB**：切带 `@XxlJob` 的方法，按 **handler 名** 计数（实现上不编译依赖 `xxl-job-core`，避免无关服务类加载问题）。

**5. 网关 `oj-gateway` 为何未用同一套 AOP**  
Gateway 基于 **WebFlux**，请求链路不是 Servlet **`@Controller`**，**同一套 MVC 切点无法覆盖**。若要对网关做 HTTP 指标，需在 **`GlobalFilter`** 等响应式组件中自行使用 **`MeterRegistry`** 记录（与 §6.2 说明一致）。

---

## 6. 本项目监控了哪些服务、哪些具体能力

### 6.1 抓取层面（Prometheus job）

以下 job 在 `prometheus.yml` 中配置，**Targets 全为 UP** 时表示抓取正常：

- `prometheus`（自身）
- `oj-gateway`
- `oj-system`
- `oj-judge`
- `oj-job`
- `oj-friend`

### 6.2 指标层面（功能）

**A. Spring Boot 默认（Actuator / Micrometer）**

- JVM 内存、GC、线程、CPU 等
- Web 层（如 `http.server.requests`，名称随版本略有差异）
- 数据源、Hikari 连接池（若引入相关 starter）

**B. 业务埋点（`oj-common-AOP`）**

| 服务 | 能力说明 | 指标名（Micrometer 名，Prometheus 中多为下划线） |
|------|-----------|--------------------------------------------------|
| oj-system | 管理端登录、新增用户（注册）次数 | `user.actions` → `user_actions_total`，标签 `module=system`，`action=login` / `register` |
| oj-friend | 注册、各渠道登录次数 | `user_actions_total`，标签 `module=friend`，`action=login` / `register` |
| oj-judge | 沙箱 **compile** / **run** 耗时与次数 | `judge.operation` → `judge_operation_seconds_count` / `_sum` / `_max` 等，标签 `phase=compile` / `run` |
| oj-job | XXL-JOB 任务每执行一次 | `xxl.job.executions` → `xxl_job_executions_total`，标签 `handler=<@XxlJob 名称>` |

**说明**：网关 **HTTP 次数/状态码** 若未单独埋点，可主要依赖 **Actuator 提供的 HTTP 指标**；业务表中的「Gateway 行」若曾规划自定义 `http_requests_total`，需后续在 Gateway 侧自行增加 Filter 埋点（与 MVC AOP 不同）。

---

## 7. 使用教程

### 7.1 确认数据源

1. 登录 Grafana（`http://localhost:3000`）。
2. 左侧 **Connections → Data sources**。
3. 选中 **Prometheus**，URL 应为 `http://prometheus:9090`（与 compose 中网段一致）；本机调试也可改为 `http://host.docker.internal:9090`（视环境而定）。
4. 底部 **Save & test**，显示绿色成功即可。

### 7.2 用 Explore 找指标（写 PromQL 前先干这件事）

1. 左侧 **Explore**。
2. 右上角选 **Prometheus**。
3. 在 **Metrics browser** 或查询框输入关键字，例如：`user_actions`、`judge_operation`、`jvm_memory`。
4. 选中一条时间序列，看右侧 **Labels**（如 `application`、`module`、`phase`），后续 Panel 里要与此一致。

### 7.3 新建大盘与 Panel

1. **Dashboards → New → New dashboard → Add visualization**。
2. **Visualization** 选 **Time series**（折线图）。
3. 下方 **Query** 选项卡：
   - Data source：**Prometheus**。
   - **A** 行中输入 PromQL（见 7.4）。
4. 右侧 **Panel options** 可改标题、单位（如 `ops`、`s`、`percent`）。
5. 右上角 **Save dashboard**。

### 7.4 PromQL 示例（直接粘贴到 Query 框）

**业务 Counter（登录/注册次数增量，建议窗口 ≥ 1m）**

```promql
rate(user_actions_total{module="system"}[5m])
```

```promql
increase(user_actions_total{module="friend",action="login"}[5m])
```

**判题 compile / run QPS**

```promql
rate(judge_operation_seconds_count{phase="compile"}[5m])
```

```promql
rate(judge_operation_seconds_count{phase="run"}[5m])
```

**判题平均耗时（秒）**

```promql
rate(judge_operation_seconds_sum{phase="compile"}[5m])
/
rate(judge_operation_seconds_count{phase="compile"}[5m])
```

**XXL 任务执行次数增量**

```promql
increase(xxl_job_executions_total[5m])
```

**注意**：不要使用短于抓取间隔的窗口（例如 `[5s]`），在 **scrape_interval=15s** 时易导致 **No data**，请改用 `[1m]` 或 `[5m]`。

### 7.5 变量（可选，实现「选应用」筛选）

1. Dashboard **Settings（齿轮）→ Variables → Add variable**。
2. 类型 **Query**，Data source **Prometheus**，Query 示例：`label_values(up, job)` 或 `label_values(user_actions_total, application)`。
3. 在 Panel 的 PromQL 中使用：`user_actions_total{application="$app"}`（变量名与引用一致）。

### 7.6 导入社区大盘（可选）

1. **Dashboards → New → Import**。
2. 输入 Grafana 官方模板 ID（如 JVM 相关 **4701** 等），选择 Prometheus 数据源。
3. 导入后根据 `application` 等标签调整筛选。

### 7.7 常见问题

| 现象 | 排查 |
|------|------|
| Panel **No data** | 窗口太短，比如监控间隔15s，promql请求5s的内容；时间范围无流量；Label 与变量不一致；服务未启动或 Target **DOWN**，|
| Prometheus **Targets DOWN** | 服务未监听对应端口；防火墙；`host.docker.internal` 不可用 |
| 只有 JVM 没有 `user_actions` | 未请求登录/注册，Counter 可能未注册；或 `oj-common-AOP` 未引入 / 自动配置未生效 |

---

## 8. 文档修订记录（摘要）

- 与仓库目录 **`oj-prometheus`**、`oj-common-AOP`、各服务 **`application.yml` 中 management 配置** 保持一致；若端口或埋点变更，请同步更新 **第 5、6 节**。
- **Actuator / Micrometer / MeterRegistry**见 **§3.2**；**AOP 织入流程**见 **§3.6**；**项目内集成步骤（依赖与配置）**见 **§5.6**。
