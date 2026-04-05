# OJ 前端静态页

LeetCode 深色风格，**HTML + CSS + JS**。

- **`index.html`**：入口，选择 **管理员模式** / **用户模式（预览）**
- **`admin.html`**：管理端联调（左侧菜单含竞赛/题目/普通用户 **原型** + **SysUser** 已接入）
- **`user.html`**：用户端 **oj-friend** 全接口联调（注册/登录/登出、竞赛列表、我的报名、报名），与网关 `token` 鉴权一致；`localStorage` 键 **`oj_api_base`** 与管理员页共用

管理端通过 **Spring Cloud Gateway**（默认 `http://localhost:9000`）调用后端。

## 功能（与 `SysUserController` 对齐）

- **登录**：`GET /sysUser/login`
- **分页列表**：`GET /sysUser/sys-users?page=`（MyBatis-Plus 分页，每页 **20** 条）
- **查询**：`GET /sysUser/findUserById?id=`、`GET /sysUser/findUserByUserAccount?userAccount=`
- **新增**：`POST /sysUser/insertUser`
- **删除**：`DELETE /sysUser/deleteUserById?id=`（列表行内删除；另有 `deleteUserByUserAccount` 未做单独表单，可按需扩展）
- **挤号 / 令牌失效**：401 或网关提示时弹窗并退回登录页

## 启动方式

1. 启动 **Redis**、**Nacos**、**oj-system**、**oj-gateway**（网关已配置开发用 **CORS**）。
2. 打开页面（任选其一）：

**Windows 一键（推荐）**：双击 **`启动前端.bat`**

- **不需要 Python / Node**：使用系统自带的 **PowerShell** 脚本 `serve-static.ps1` 起静态服务（默认 **`8765`** 端口，可在 bat 里改 `PORT=`）。
- 若浏览器未自动打开，请手动访问 **`http://127.0.0.1:8765/`**。

**仍打不开**：双击 **`诊断前端环境.bat`**；若提示端口监听失败，可换一个 `PORT` 或以管理员身份试一次（少数机器需 `urlacl`）。

**可选（已装 Node 时）**：

```bash
cd front-end
npx --yes serve@14 . -l 8765
```

**仅双击 `index.html`**：可以用浏览器打开，但部分环境下 `file://` 访问网关可能被 **CORS** 拦截，联调失败时请用上面两种方式。

3. 页面顶部可修改 **网关地址**（持久化到 `localStorage`）。

## oj-friend（`user.html` + `js/friend-api.js`）

经网关前缀（默认 `http://localhost:9000`）：

| 接口 | 说明 |
|------|------|
| `GET /ping` | 健康检查（部分部署未在网关路由 `/ping`，失败可忽略） |
| `POST /friend/user/register` | 注册 |
| `POST /friend/user/loginByPhonePassword` | 手机+密码 |
| `POST /friend/user/loginByEmailPassword` | 邮箱+密码 |
| `POST /friend/user/loginByEmailCode` | 邮箱验证码登录（仅返回 token） |
| `POST /friend/user/sendEmailCode` | 发验证码 |
| `GET /friend/user/detail?userId=` | 用户详情（需登录态） |
| `POST /friend/user/logout` | 登出（body 需 `userId`） |
| `GET /friend/exam/list/active` | 未开始+进行中竞赛 |
| `GET /friend/exam/list/finished` | 已结束竞赛 |
| `GET /friend/exam/my/registrations` | 我已报名 |
| `POST /friend/exam/registerExam` | 报名（body: `{ examId }`） |

会话保存在 **`sessionStorage`** 键 **`oj_friend_session`**（含 `token`、`userId` 等）。

## 说明

- 若直接双击打开 `index.html`（`file://`），部分浏览器可能限制跨域；请优先用上述静态服务器。
- `createBy` / `updateBy` 由网关注入请求头，前端无需填写。
