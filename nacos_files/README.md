# Nacos 配置中心 — 示例配置说明

本目录下的文件与项目 `bootstrap.yml` 中引用的 **Data ID** 一一对应，可直接在 Nacos 控制台 **「配置管理 → 配置列表」** 中新建配置，将内容粘贴进去。

**通用约定**

| 项 | 值 |
|----|-----|
| Group | `DEFAULT_GROUP`（与代码里一致；可按环境改为 `DEV`/`PROD` 等，需同步改 bootstrap） |
| 命名空间 | 默认 `public`；若使用自定义 namespace，在各自 `bootstrap` 里配置 `namespace` |

---

## Data ID 一览

| Data ID | 格式 | 谁在用 | 说明 |
|---------|------|--------|------|
| `common-datasource.yaml` | YAML | `oj-system`、`oj-judge`、`oj-friend`（shared-config） | MySQL + **HikariCP** 数据源 |
| `common-mybatis.yaml` | YAML | `oj-system`、`oj-judge`、`oj-friend`（shared-config） | **MyBatis-Plus** 公共项 |
| `common-redis.yaml` | YAML | `oj-system`、`oj-judge`、`oj-friend`、`oj-gateway`（shared-config/import） | **Redis**（登录态等） |
| `common-elasticsearch.yaml` | YAML | `oj-system`、`oj-friend`（shared-config） | **Elasticsearch** 连接（`spring.elasticsearch.uris`） |
| `oj-system.yaml` | YAML | `oj-system`（extension-config） | 系统服务端口等专属配置 |
| `oj-judge.yaml` | YAML | `oj-judge`（extension-config） | 判题服务端口、Docker 镜像配置 |
| `oj-job.yaml` | YAML | `oj-job`（extension-config） | **MyBatis-Plus**（`com.bite.job.domain`）、**XXL-JOB** 执行器 |
| `oj-gateway.yml` | YML | `oj-gateway`（`spring.config.import`） | **Gateway** 路由、负载均衡等 |
| `oj-friend.yml` | YAML | `oj-friend`（extension-config） | 用户服务端口、邮件配置等 |

> `oj-gateway` 的 Data ID 由 `prefix` + `.` + `file-extension` 组成，当前为 **`oj-gateway.yml`**（注意后缀是 **yml**）。

---

## 导入步骤（简要）

1. 启动 Nacos Server（默认 `8848`）。
2. 对每个 Data ID：**新建配置** → 填写 Data ID、Group、格式（YAML）→ 粘贴对应文件内容 → 发布。
3. 将库名、账号密码、Redis 地址等改成你本机/环境真实值。
4. 重启 `oj-system`、`oj-gateway`（或依赖 `refresh: true` 的配置项触发刷新）。

---

## 与代码的对应关系

- `oj-modules/oj-system/src/main/resources/bootstrap.yml`
  → `shared-configs`：`common-datasource.yaml`、`common-mybatis.yaml`、`common-redis.yaml`、`common-elasticsearch.yaml`
  → `extension-configs`：`oj-system.yaml`
- `oj-modules/oj-judge/src/main/resources/bootstrap.yml`
  → `shared-configs`：`common-datasource.yaml`、`common-mybatis.yaml`、`common-redis.yaml`
  → `extension-configs`：`oj-judge.yaml`
- `oj-modules/oj-friend/src/main/resources/bootstrap.yml`
  → `shared-configs`：`common-datasource.yaml`、`common-mybatis.yaml`、`common-redis.yaml`、`common-elasticsearch.yaml`
  → `extension-configs`：`oj-friend.yaml`
- `oj-modules/oj-job/src/main/resources/bootstrap.yml`
  → `shared-configs`：`common-datasource.yaml`、`common-redis.yaml`（**不含** `common-mybatis`，避免实体包名指向 system）
  → `extension-configs`：`oj-job.yaml`
- `oj-gateway/src/main/resources/bootstrap.yml`
  → `spring.config.import`：`optional:nacos:oj-gateway.yml`（由 `prefix` + `file-extension` 决定）

网关本地仍保留 **Redis / JWT / token 滑动续期** 等配置时，会与 Nacos 中的 `oj-gateway.yml` **合并**；若你希望全部上云，可把 `bootstrap.yml` 里重复段删掉，只保留 Nacos。

---

## 安全提示

- 生产环境务必修改默认数据库密码、`JWT_SECRET`、`PWD_SALT`。
- 不要把含真实密码的配置提交到公开仓库；可用占位符 + 环境变量注入。

## 网关白名单（`oj-gateway.yml` / `bootstrap.yml`）

- 配置项：`security.whitelist.paths`（逗号分隔的**完整路径**）。
- 默认放行：`/sysUser/login`（无需 token）。`insertUser` 需带 token，由网关写入请求头 `X-User-Id` 供 `createBy`/`updateBy`。
- 修改后需重启网关，或后续可为过滤器加 `@RefreshScope` 以支持 Nacos 动态刷新。
