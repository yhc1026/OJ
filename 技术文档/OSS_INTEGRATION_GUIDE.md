# OJ-Ssystem OSS 集成操作指南（含问题复盘）

本文档用于沉淀本项目 OSS 集成方案、配置步骤、联调流程与常见问题排查。

---

## 0. 八个高频踩坑点（强烈建议先看）

1) 绝对不能用主账号 AK/SK 直接调用 STS  

- 主账号调用 STS 常见报错：`Roles may not be assumed by root accounts`
- 必须改为 RAM 子用户（调用者）AK/SK

2) STS 必须两套权限：调用者权限 + 角色权限（两个授权）  

- 调用者（RAM 用户）需要 `sts:AssumeRole` 权限
- 被扮演角色本身也需要 OSS 资源权限（PutObject 等）

3) 角色信任策略必须写死子用户 ARN  

- 建议在信任策略里明确允许的调用主体 ARN（最小信任面）
- 不建议宽泛信任，避免越权

4) 前端直传 100% 要配 OSS CORS 跨域  

- 浏览器直传依赖跨域放行
- 未配置 CORS 时前端会在浏览器侧被拦截

5) OSS 开启“禁止公共读”会导致所有图片直链无法访问  

- 私有读 bucket 下，普通拼接 URL 通常 403
- 需要签名 URL 或后端代理流

6) 上传成功但头像不显示，99% 是私有文件无访问权限  

- 文件在 OSS 存在不代表浏览器可读
- 优先检查预览 URL 的 HTTP 状态码（常见 403）

7) STS 临时 Token 只继承角色权限，不继承子用户权限  

- 子用户自身的 OSS 权限不会自动带入 STS 临时凭证
- 最终生效权限以角色策略为准

8) 路径必须严格匹配 Resource 权限  

- OSS 策略中的 Resource 前缀要与实际 `objectKey` 严格一致
- 前缀不匹配会出现“上传失败/无权限”

---

## 1. 当前落地方案概览

本项目当前使用的是「前端 STS 直传 OSS + 后端校验入库 + 后端代理预览」方案。

- 前端先请求后端获取 STS 临时凭证
- 前端使用 STS 直传图片到 OSS 限定目录
- 前端把 `objectKey` 回传后端
- 后端校验 `objectKey` 合法性后写入 `tb_user.head_image`
- 前端查看头像时，优先走后端代理流接口进行稳定预览

---

## 2. 代码结构与关键文件

### 2.1 公共模块 `oj-common-file`

- 自动装配注册
  - `oj-common/oj-common-file/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 配置类
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/config/OssProperties.java`
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/config/OssAutoConfiguration.java`
- 存储与 STS 服务
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/service/FileStorageService.java`
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/service/OssStsService.java`
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/service/impl/OssFileStorageServiceImpl.java`
  - `oj-common/oj-common-file/src/main/java/com/bite/common/file/service/impl/AliyunOssStsServiceImpl.java`
- 兜底实现（配置未生效时防止服务启动失败）
  - `DisabledFileStorageServiceImpl`
  - `DisabledOssStsServiceImpl`

### 2.2 业务模块 `oj-friend`

- 用户头像业务接口
  - `oj-modules/oj-friend/src/main/java/com/bite/friend/controller/UserOperationController.java`
  - `oj-modules/oj-friend/src/main/java/com/bite/friend/service/FriendAuthService.java`
  - `oj-modules/oj-friend/src/main/java/com/bite/friend/service/impl/FriendAuthServiceImpl.java`
- 头像代理预览接口
  - `oj-modules/oj-friend/src/main/java/com/bite/friend/controller/UserFileController.java`

### 2.3 前端 `front-end`

- API 封装
  - `front-end/js/friend-api.js`
- 页面与交互
  - `front-end/user.html`
  - `front-end/js/user.js`

---

## 3. Nacos 配置说明

本地模板文件：`nacos_files/common-oss.yaml`

建议配置：

```yaml
oss:
  enabled: true
  endpoint: oss-cn-chengdu.aliyuncs.com
  access-key-id: ${OSS_ACCESS_KEY_ID:}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
  bucket-name: oj-study-test-yhc
  base-dir: oj/
  region: oss-cn-chengdu
  sts-role-arn: acs:ram::<your-account-id>:role/<your-role-name>
  sts-role-session-name: oj-friend-avatar
  sts-duration-seconds: 900
  cdn-domain: ${OSS_CDN_DOMAIN:}
```

注意：

- `endpoint` 需与 bucket 地域一致
- `sts-role-arn` 为必填（STS 场景）
- 建议使用环境变量注入 AK/SK，不要写死到仓库

---

## 4. 启动侧配置要点

`oj-friend` 需要确保读取 OSS 配置：

- `shared-configs` 中包含 `common-oss.yaml`
- 同时已加入显式导入兜底：
  - `spring.config.import: optional:nacos:common-oss.yaml?group=DEFAULT_GROUP`

若仍未读取，多半是 Nacos 的 `Data ID / Group / Namespace` 与运行实例不一致。

---

## 5. RAM 与 STS 配置要点

## 5.1 不要用主账号 root AK 调 STS

若出现：

- `NoPermission : Roles may not be assumed by root accounts`

说明使用了主账号 AK/SK 调 STS。应改为：

- 创建 RAM 用户（调用者）
- 给该 RAM 用户授权 `sts:AssumeRole`
- 使用该 RAM 用户的 AK/SK 作为 `oss.access-key-id / oss.access-key-secret`

## 5.2 `oss.sts-role-arn` 格式

```text
acs:ram::<account-id>:role/<role-name>
```

## 5.3 角色策略最小化

- 信任策略允许调用者 RAM 用户 AssumeRole
- 权限策略限制到目标 bucket 的指定前缀（如 `oj/avatar/*`）

---

## 6. 头像业务接口清单（当前）

## 6.1 申请 STS

- `POST /friend/user/avatar/sts`
- 需携带 token（经 Gateway 鉴权）
- 返回 STS 临时凭证与允许上传前缀

## 6.2 回传 objectKey 入库

- `PUT /friend/user/avatar/object-key`
- 入参：`objectKey`
- 后端校验 key 合法性（必须在当前用户前缀下）
- 成功后更新 `tb_user.head_image`
- 同时删除 token-详情缓存键，避免脏缓存

## 6.3 查看头像信息

- `GET /friend/user/avatar`
- 返回：
  - `objectKey`
  - `url`（优先签名 URL，必要时回退普通 URL）

## 6.4 稳定预览代理流

- `GET /friend/file/my-avatar/content`
- 前端带 token 请求后端，由后端从 OSS 读取字节并返回图片流
- 用于绕过 OSS 私有读、防盗链、签名时效等导致的前端直链预览失败

---

## 7. 前端流程（当前）

1. 点击上传头像
2. 请求 `POST /friend/user/avatar/sts`
3. 前端用 OSS SDK 直传到 `objectKeyPrefix` 下
4. 回传 `PUT /friend/user/avatar/object-key`
5. 刷新头像：
   - 先拉 `GET /friend/user/avatar`
   - 再通过 `GET /friend/file/my-avatar/content` 获取稳定预览

---

## 8. 本次 OSS 问题汇总与修复记录

以下问题均为本项目本次集成过程中真实出现。

1) `No qualifying bean of type FileStorageService`

- 原因：`oj-common-file` 的自动装配未被识别
- 修复：增加 `AutoConfiguration.imports` 注册 `OssAutoConfiguration`

2) `ArrayStoreException ... jakarta.activation.MimeTypeRegistry`

- 原因：Boot 3 的 `jakarta.*` 与旧 `javax.activation/jaxb` 依赖冲突
- 修复：移除 `javax.xml.bind/jaxb-runtime/activation` 旧依赖

3) `申请 STS 失败: OSS 未启用或 STS 未配置`

- 原因：运行时未读到有效 `oss.*` 配置（Nacos 未生效/配置不一致）
- 修复：
  - 增加显式 Nacos 导入
  - 增加配置缺失项诊断信息，直接返回缺失字段

4) 明明配置了 Nacos 仍走 disabled 服务

- 原因：自动装配条件与配置加载时序导致注入兜底实现
- 修复：在业务层增加临时 fallback 判断（配置齐全则直连 STS）

5) `NoPermission : Roles may not be assumed by root accounts`

- 原因：主账号 root AK/SK 调用 STS
- 修复：改用 RAM 用户 AK/SK + 正确的 AssumeRole 授权与信任策略

6) 头像预览失败

- 可能原因：
  - Bucket 非公共读导致直链 403
  - 签名 URL 失效
  - 浏览器直链策略/防盗链影响
- 修复：
  - 返回签名 URL
  - 增加后端代理图片流接口用于稳定预览

7) 头像更新后缓存不一致

- 原因：token-详情缓存仍是旧值
- 修复：头像更新成功后删除 token-详情键

---

## 9. 排查建议（生产可复用）

当 OSS 功能异常时，建议按以下顺序检查：

1. 先看后端返回码与消息（是否权限、配置、参数问题）
2. 检查 Nacos 三要素：
   - Data ID
   - Group
   - Namespace
3. 检查 STS 必填项：
   - `oss.enabled`
   - `oss.sts-role-arn`
   - `oss.access-key-id`
   - `oss.access-key-secret`
   - `oss.bucket-name`
   - `oss.endpoint`
4. 检查 RAM 权限模型（调用者与角色信任关系）
5. 通过浏览器 Network 判断头像预览请求是 401/403/404 还是超时

---

## 10. 安全建议

- 不在仓库中明文存储 AK/SK
- 优先使用 RAM 子账号 + 最小权限
- STS 时效建议短期（如 900 秒）
- object key 必须服务端校验前缀与字符合法性

---

如后续需要，可再补充：

- 头像上传大小与格式统一校验规范
- RAM 策略 JSON 模板（信任策略 + 最小权限策略）
- 故障排查流程图（服务端、网关、Nacos、OSS、前端网络五泳道）
