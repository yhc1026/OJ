# OJ 判题系统技术文档

## 1. 为什么需要 Docker 沙箱判题？

在 OJ 系统中，用户的代码是**不可信的**——它们由外部用户提交，可能会包含恶意代码或危险的编程错误。如果直接在服务器系统环境下执行这些代码，将面临严重的安全风险。

### 1.1 直接在系统环境判题的风险

| 风险类型 | 危害 | 示例 |
|----------|------|------|
| **系统文件破坏** | 删除或篡改服务器上的关键文件 | `rm -rf /` 或格式化磁盘 |
| **服务中断** | 耗尽系统资源（CPU、内存、磁盘）导致服务崩溃 | 无限循环、无限递归、内存泄漏 |
| **数据泄露** | 读取服务器上的敏感数据 | 读取其他用户的代码、数据库凭证 |
| **网络攻击** | 对内网或外网发起攻击 | 发起 DDoS、扫描内网端口 |
| **挖矿/僵尸网络** | 利用服务器算力进行非法活动 | 植入加密货币挖矿程序 |
| **权限提升** | 尝试获取更高系统权限 | 利用漏洞提权 |

### 1.2 Docker 沙箱的核心作用

Docker 容器提供**资源隔离**和**权限限制**，使不可信代码在受控环境中执行：

```
┌─────────────────────────────────────────────────────────┐
│                     宿主机 (Host)                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │           Docker 容器 (Sandbox)                  │   │
│  │                                                  │   │
│  │   ┌──────────────┐    ┌──────────────────┐     │   │
│  │   │  用户代码     │───►│  编译 & 运行      │     │   │
│  │   │  (不可信)    │    │  (隔离环境)       │     │   │
│  │   └──────────────┘    └──────────────────┘     │   │
│  │                                                  │   │
│  │   限制:                                         │   │
│  │   • 无宿主机的 root 权限                        │   │
│  │   • 无法访问宿主机文件系统                       │   │
│  │   • 无法访问内网资源                            │   │
│  │   • 资源受限 (CPU/内存/磁盘)                    │   │
│  │   • 网络隔离或严格限制                          │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   MySQL     │  │   Redis     │  │   文件存储   │   │
│  │   (隔离)    │  │   (隔离)    │  │   (隔离)    │   │
│  └─────────────┘  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 1.3 Docker 提供的安全隔离

| 隔离层 | 说明 | 实际配置 |
|--------|------|----------|
| **文件系统隔离** | 容器内的操作不影响宿主机 | Docker 的 UnionFS，容器内 `/` 仅映射到容器内 |
| **网络隔离** | 默认无网络访问，防止攻击扩散 | `--network none` 或严格防火墙规则 |
| **资源限制** | 限制 CPU、内存、磁盘使用 | Docker cgroups 限制 |
| **用户权限限制** | 容器内通常是普通用户，非 root | `-u` 参数指定用户 |
| **只读文件系统** | 关键目录设为只读 | `--read-only` 或 `:ro` 挂载 |
| **自动清理** | 容器退出后自动销毁，防止残留 | `--rm` 参数 |

### 1.4 典型危险代码示例

以下代码**在沙箱中会被阻止**，但在直接系统环境下**会造成严重后果**：

```java
// 危险1: 删除整个文件系统
public class Malicious1 {
    public static void main(String[] args) {
        // 在沙箱外执行会删除系统文件
        Runtime.getRuntime().exec("rm -rf /");
    }
}

// 危险2: 无限资源消耗
public class Malicious2 {
    public static void main(String[] args) {
        // 无限创建线程耗尽 CPU
        while(true) {
            new Thread(() -> {}).start();
        }
    }
}

// 危险3: 读取服务器敏感文件
public class Malicious3 {
    public static void main(String[] args) throws Exception {
        // 读取数据库配置文件
        String config = new String(Files.readAllBytes(
            Paths.get("/etc/mysql/my.cnf")
        ));
        System.out.println(config);
    }
}
```

### 1.5 沙箱判题 vs 直接判题对比

| 对比项 | 直接系统判题 | Docker 沙箱判题 |
|--------|--------------|-----------------|
| **安全性** | ❌ 高风险 | ✅ 安全隔离 |
| **资源控制** | ❌ 难以限制 | ✅ 可精确限制 |
| **环境一致性** | ❌ 依赖宿主机环境 | ✅ 标准化镜像 |
| **部署灵活性** | ❌ 紧耦合 | ✅ 可迁移部署 |
| **并发能力** | ❌ 受限于单机资源 | ✅ 可水平扩展 |
| **故障恢复** | ❌ 复杂 | ✅ 容器自动重启 |

### 1.6 总结

> **在 OJ 系统中，永远不要在服务器系统环境下直接运行用户提交的代码。**
>
> Docker 沙箱通过多层隔离机制，确保即使代码包含恶意行为或严重错误，也不会影响宿主机和其他服务。这是构建安全、可靠的在线判题系统的**必备基础设施**。

---

## 2. 系统概述

OJ 判题系统采用**前后分离架构**，由两个独立微服务组成：

| 模块 | 端口 | 职责 |
|------|------|------|
| `oj-friend` | 9204 | 用户端：接收代码提交、存储提交记录 |
| `oj-judge` | 9202 | 判题服务：代码编译、Docker 沙箱执行、结果判定 |

```
┌─────────────┐     Feign RPC      ┌─────────────┐
│  oj-friend  │ ────────────────► │  oj-judge   │
│  (9204)     │                   │  (9202)     │
│             │                   │             │
│ 用户提交    │                   │ Docker 编译 │
│ 代码存储    │ ◄────────────────  │ 沙箱运行    │
└─────────────┘   返回判题结果    └─────────────┘
```

---

---

## 3. 判题流程

### 3.1 完整调用链路

```
1. 用户 POST /friend/judge/submit
       │
       ▼
2. FriendJudgeService.submit()
       │  接收 CodeSubmitRequest
       │  获取题目信息 (mainMethod, questionCase, expectedResult)
       ▼
3. 创建 tb_friend_code_submit 记录 (status=PENDING)
       │
       ▼
4. 通过 JudgeFeignClient 调用 oj-judge
       │
       ▼
5. JudgeRunServiceImpl.run() 执行判题
       │
       ├── 步骤1: 拼接代码 (userCode + mainMethod)
       ├── 步骤2: 写入 Java 源文件 (Main.java)
       ├── 步骤3: 写入测试用例 (input.txt)
       ├── 步骤4: 写入期望输出 (expected.txt)
       ├── 步骤5: Docker 编译 (javac)
       ├── 步骤6: Docker 运行 (java)
       └── 步骤7: 比较结果 (expected.txt vs output.txt)
       │
       ▼
6. 返回 JudgeSingleCaseResponse
       │
       ▼
7. 更新 tb_friend_code_submit 记录
       │  score = verdict == 0 ? 1 : 0
       │  status = PASS(0) / FAIL(1)
       ▼
8. 返回 CodeSubmitResultVo 给用户
```

### 3.2 判题结果枚举

| 枚举值 | code | 说明 |
|--------|------|------|
| `ACCEPTED` | 0 | 通过 |
| `WRONG_ANSWER` | 1 | 答案错误 |
| `COMPILE_ERROR` | 2 | 编译错误 |
| `RUNTIME_ERROR` | 3 | 运行时错误 |
| `TIME_LIMIT` | 4 | 超时 |
| `MEMORY_LIMIT` | 5 | 内存超限 |
| `INTERNAL_ERROR` | 99 | 系统内部错误 |

---

## 4. 沙箱执行详解

本节详细说明 oj-judge 如何在 Docker 沙箱中执行用户代码。

### 4.1 工作目录结构

判题服务使用临时目录 `tmp-judge/oj-judge-{uuid}/` 作为工作空间，所有文件操作都在此目录下进行：

```
tmp-judge/
└── oj-judge-abc123/
    ├── Main.java        # 用户代码 + main 方法拼接后的完整源码
    ├── Main.class      # 编译生成的字节码文件
    ├── input.txt       # 测试用例输入
    ├── expected.txt    # 期望输出（标准答案）
    └── output.txt      # 程序实际输出
```

### 4.2 文件生成方式

#### 4.2.1 代码拼接与 Java 文件生成

**源码拼接规则**：将用户提交的 `userCode` 与题目配置的 `mainMethod` 直接拼接。

```
┌─────────────────────────────────────────┐
│  userCode (用户提交)                     │
│  ┌───────────────────────────────────┐  │
│  │ public class Main {               │  │
│  │     public int add(int a, int b) {│  │
│  │         return a + b;             │  │
│  │     }                             │  │
│  │ }                                 │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              + 拼接
┌─────────────────────────────────────────┐
│  mainMethod (题目配置)                   │
│  ┌───────────────────────────────────┐  │
│  │ public static void main(String[]  │  │
│  │     args) {                       │  │
│  │     Main m = new Main();          │  │
│  │     Scanner sc = new Scanner(     │  │
│  │         System.in);               │  │
│  │     int a = sc.nextInt();         │  │
│  │     int b = sc.nextInt();         │  │
│  │     System.out.println(m.add(a,b));│  │
│  │ }                                 │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**代码实现**：

```java
// ConnectCodeAndOutput.java
public String connectCode(String userCode, String mainMethod) {
    return userCode + "\n\n" + mainMethod;
}

public void outputJava(Path path, String finalCode) throws IOException {
    path = path.resolve("Main.java");
    Files.writeString(path, finalCode, StandardCharsets.UTF_8);
}
```

#### 4.2.2 测试用例文件

测试用例从题目配置 `questionCase` 字段读取，直接写入 `input.txt`：

```java
public void outputTest(Path path, String testInput) throws IOException {
    path = path.resolve("input.txt");
    Files.writeString(path, testInput, StandardCharsets.UTF_8);
}
```

#### 4.2.3 期望输出文件

期望输出从题目配置 `expectedResult` 字段读取，写入 `expected.txt`：

```java
public void outputExpectedResult(Path path, String expected) throws IOException {
    path = path.resolve("expected.txt");
    Files.writeString(path, expected, StandardCharsets.UTF_8);
}
```

### 4.3 编译过程

#### 4.3.1 编译原理

使用 `javac` 编译器将 Java 源码编译为字节码文件 `Main.class`。

#### 4.3.2 Docker 编译命令

```bash
docker run --rm \
  -v "/path/to/tmp-judge/oj-judge-abc123:/workspace" \
  -w /workspace \
  eclipse-temurin:17-jdk-alpine \
  javac -d /workspace/classes /workspace/Main.java
```

| 参数 | 说明 |
|------|------|
| `--rm` | 容器运行完毕后自动删除 |
| `-v` | 将宿主机目录挂载到容器内，实现文件共享 |
| `-w /workspace` | 设置工作目录为 `/workspace` |
| `eclipse-temurin:17-jdk-alpine` | JDK 17 轻量级镜像 |
| `javac -d /workspace/classes` | 编译并输出到 classes 目录 |

#### 4.3.3 Java 实现

```java
// RunAndOutput.java
public boolean compile(Path javaFilePath, Path outputPath) throws Exception {
    String command = String.format(
        "docker run --rm -v \"%s:/workspace\" -w /workspace " +
        "eclipse-temurin:17-jdk-alpine javac -d /workspace/classes /workspace/Main.java",
        outputPath.toAbsolutePath().toString().replace("\\", "/")
    );

    Process process = Runtime.getRuntime().exec(command);
    int exitCode = process.waitFor();

    if (exitCode == 0) {
        return true;  // 编译成功
    } else {
        // 读取错误信息
        return false; // 编译失败
    }
}
```

### 4.4 运行过程

#### 4.4.1 运行原理

使用 `java` 命令执行编译后的字节码文件，并通过标准输入重定向读取测试用例。

#### 4.4.2 Docker 运行命令

```bash
docker run --rm \
  -v "/path/to/tmp-judge/oj-judge-abc123:/workspace" \
  -w /workspace \
  eclipse-temurin:17-jdk-alpine \
  sh -c "java -cp classes Main < /workspace/input.txt > /workspace/output.txt"
```

| 参数 | 说明 |
|------|------|
| `-cp classes` | 指定类路径为 classes 目录 |
| `Main` | 主类名（对应 Main.class） |
| `< /workspace/input.txt` | 将 input.txt 内容作为标准输入 |
| `> /workspace/output.txt` | 将标准输出重定向到 output.txt |

#### 4.4.3 Java 实现

```java
// RunAndOutput.java
public boolean run(Path javaClassPath, Path outputPath, Path inputPath) throws IOException {
    String command = String.format(
        "docker run --rm -v \"%s:/workspace\" -w /workspace " +
        "eclipse-temurin:17-jdk-alpine sh -c \"java -cp classes Main < /workspace/input.txt\"",
        outputPath.toAbsolutePath().toString().replace("\\", "/")
    );

    Process process = Runtime.getRuntime().exec(command);

    // 读取程序输出
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
    }

    int exitCode = process.waitFor();

    if (exitCode == 0) {
        // 将输出写入 output.txt
        Path resultFile = outputPath.resolve("output.txt");
        Files.writeString(resultFile, output.toString(), StandardCharsets.UTF_8);
        return true;
    } else {
        return false; // 运行失败
    }
}
```

### 4.5 判题（结果比较）

#### 4.5.1 判题原理

读取 `expected.txt`（期望输出）和 `output.txt`（实际输出），进行**规范化比较**。

#### 4.5.2 规范化比较算法

```java
// JudgeAndOutput.java
private boolean compare(String expected, String actual) {
    // 规范化处理：去除首尾空格 + 合并连续空格为单个空格
    String normExpected = expected.trim().replaceAll("\\s+", " ");
    String normActual = actual.trim().replaceAll("\\s+", " ");

    return normExpected.equals(normActual);
}
```

**规范化示例**：

| 期望输出 | 实际输出 | 规范化后期望 | 规范化后实际 | 比较结果 |
|----------|----------|--------------|--------------|----------|
| `Hello World` | `Hello   World` | `Hello World` | `Hello World` | ✅ 通过 |
| `1 2 3` | `1 2 3 ` | `1 2 3` | `1 2 3` | ✅ 通过 |
| `abc` | `ABC` | `abc` | `ABC` | ❌ 不通过 |

#### 4.5.3 Java 实现

```java
// JudgeAndOutput.java
public boolean judgeCode(Path path) throws IOException {
    // 读取文件
    Path expectedResPath = path.resolve("expected.txt");
    Path outputResPath = path.resolve("output.txt");

    String expected = Files.readString(expectedResPath);
    String actual = Files.readString(outputResPath);

    // 比较结果
    return compare(expected, actual);
}
```

### 4.6 完整执行流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     判题执行完整流程                              │
└─────────────────────────────────────────────────────────────────┘

1. 创建临时工作目录
   tmp-judge/oj-judge-{uuid}/
          │
          ▼
2. 生成 Main.java (用户代码 + main方法)
   ┌─────────────────────────────────────────┐
   │ public class Main {                     │
   │     public int add(int a, int b) {      │
   │         return a + b;                   │
   │     }                                   │
   │ }                                       │
   │                                         │
   │ public static void main(String[] args){ │
   │     Main m = new Main();                │
   │     Scanner sc = new Scanner(System.in);│
   │     int a = sc.nextInt();               │
   │     int b = sc.nextInt();               │
   │     System.out.println(m.add(a,b));     │
   │ }                                       │
   └─────────────────────────────────────────┘
          │
          ▼
3. 生成 input.txt (测试用例)
   ┌─────────┐
   │ 1 2     │
   └─────────┘
          │
          ▼
4. 生成 expected.txt (期望输出)
   ┌─────────┐
   │ 3       │
   └─────────┘
          │
          ▼
5. Docker 编译
   ┌────────────────────────────────────────────────────────┐
   │ docker run --rm -v ... eclipse-temurin:17-jdk-alpine  │
   │ javac -d /workspace/classes /workspace/Main.java       │
   └────────────────────────────────────────────────────────┘
          │
          ▼ (生成 Main.class)
6. Docker 运行
   ┌────────────────────────────────────────────────────────┐
   │ docker run --rm -v ... eclipse-temurin:17-jdk-alpine   │
   │ sh -c "java -cp classes Main < /workspace/input.txt"   │
   └────────────────────────────────────────────────────────┘
          │
          ▼ (生成 output.txt)
7. 比较结果
   ┌─────────────────┐     ┌─────────────────┐
   │  expected.txt   │ VS  │  output.txt     │
   │      "3"        │     │      "3"        │
   └─────────────────┘     └─────────────────┘
          │                       │
          └─────────┬─────────────┘
                    ▼
              ┌───────────┐
              │  比较结果  │
              │  ✅ AC    │
              └───────────┘
```

---

---

## 5. 核心接口

### 5.1 代码提交接口

**请求**
```
POST /friend/judge/submit
Content-Type: application/json
token: {user_token}
X-User-Id: {user_id}
```

**请求体**
```json
{
  "questionId": 1234567890,
  "examId": 1234567890,        // 可选，竞赛内提交时需要
  "code": "public class Main { ... }",
  "language": 0               // 0=Java (当前仅支持Java)
}
```

**响应**
```json
{
  "code": 1000,
  "msg": "success",
  "data": {
    "submitId": 123,
    "questionId": 1234567890,
    "questionCase": "[{\"input\":\"...\",\"output\":\"...\"}]",
    "status": 0,              // 0=通过, 1=失败, 2=待判
    "score": 1,
    "exeMessage": null
  }
}
```

### 5.2 判题服务内部接口

**请求**
```
POST /judge/run
Content-Type: application/json
```

**请求体**
```json
{
  "userCode": "public class Main { ... }",
  "mainMethod": "public static void main(String[] args) { ... }",
  "testInput": "1 2 3",
  "expectedOutput": "6",
  "timeLimitMs": 5000,
  "spaceLimitKb": 262144,
  "language": 0
}
```

**响应**
```json
{
  "code": 1000,
  "msg": "ok",
  "data": {
    "verdict": 0,
    "message": "通过",
    "actualOutput": "6"
  }
}
```

---

## 6. 核心代码结构

### 6.1 oj-judge 模块

```
oj-judge/src/main/java/com/bite/judge/
├── controller/
│   └── JudgeInternalController.java      # 判题 HTTP 接口
├── domain/dto/friend/
│   ├── JudgeRunRequest.java             # 判题请求 DTO
│   └── JudgeSingleCaseResponse.java     # 单用例判题结果
├── compose/
│   ├── ConnectCodeAndOutput.java        # 代码拼接与文件输出
│   └── OutputNormalizer.java           # 输出规范化
├── sandbox/
│   ├── RunAndOutput.java               # Docker 编译与运行
│   └── JudgeAndOutput.java             # 结果比较
└── service/
    ├── JudgeRunService.java            # 判题服务接口
    └── impl/JudgeRunServiceImpl.java   # 判题服务实现
```

#### 6.1.1 代码拼接 (ConnectCodeAndOutput)

```java
// 拼接用户代码和 main 方法
public String connectCode(String userCode, String mainMethod) {
    return userCode + "\n\n" + mainMethod;
}

// 输出到工作目录
outputJava(path, code);           // Main.java
outputTest(path, testInput);      // input.txt
outputExpectedResult(path, expected); // expected.txt
```

#### 6.1.2 Docker 编译运行 (RunAndOutput)

```java
// 编译：使用 Docker + javac
String command = "docker run --rm -v /workspace -w /workspace eclipse-temurin:17-jdk-alpine javac Main.java";
Process process = Runtime.getRuntime().exec(command);

// 运行：使用 Docker + java，支持标准输入重定向
String command = "docker run --rm -v /workspace -w /workspace eclipse-temurin:17-jdk-alpine sh -c \"java -cp classes Main < input.txt\"";
```

#### 6.1.3 结果比较 (JudgeAndOutput)

```java
// 读取期望输出和实际输出
Path expectedResPath = path.resolve("expected.txt");
Path outputResPath = path.resolve("output.txt");
String expected = Files.readString(expectedResPath);
String actual = Files.readString(outputResPath);

// 宽松比较：去空格、合并连续空格
String normExpected = expected.trim().replaceAll("\\s+", " ");
String normActual = actual.trim().replaceAll("\\s+", " ");
return normExpected.equals(normActual);
```

### 6.2 oj-friend 模块

```
oj-friend/src/main/java/com/bite/friend/
├── controller/
│   └── CodeJudgeController.java         # 代码提交入口
├── domain/
│   ├── FriendCodeSubmit.java           # 代码提交实体
│   ├── FriendQuestion.java             # 题目实体
│   ├── dto/
│   │   ├── CodeSubmitRequest.java      # 提交请求 DTO
│   │   └── JudgeRunRequest.java         # 判题请求 DTO (转发给 oj-judge)
│   └── vo/
│       └── CodeSubmitResultVo.java     # 提交结果 VO
├── feign/
│   └── JudgeFeignClient.java           # 调用 oj-judge 的 Feign 客户端
├── judge/
│   ├── QuestionCaseParser.java         # 解析 question_case JSON
│   └── QuestionTestCase.java           # 测试用例 POJO
└── service/
    ├── FriendJudgeService.java         # 判题服务接口
    └── impl/FriendJudgeServiceImpl.java # 判题服务实现
```

#### 6.2.1 数据库表结构

**tb_friend_code_submit**
| 字段 | 类型 | 说明 |
|------|------|------|
| submit_id | BIGINT | 主键 |
| user_id | BIGINT | 提交用户 |
| question_id | BIGINT | 题目 ID |
| exam_id | BIGINT | 竞赛 ID（可选） |
| user_code | TEXT | 用户代码 |
| language | INT | 语言（0=Java） |
| score | INT | 得分（0 或 1） |
| status | INT | 状态（0=通过, 1=失败, 2=待判） |
| exe_message | VARCHAR | 执行消息 |

---

## 7. 配置说明

### 7.1 Nacos 配置

**oj-judge.yaml**
```yaml
server:
  port: 9202

oj:
  judge:
    docker-image: eclipse-temurin:17-jdk-jammy
```

**shared-configs (公共配置)**
- `common-datasource.yaml` - MySQL 数据源
- `common-mybatis.yaml` - MyBatis-Plus 配置
- `common-redis.yaml` - Redis 连接

### 7.2 环境变量

| ��量 | 默认值 | 说明 |
|------|--------|------|
| `NACOS_SERVER` | localhost:8848 | Nacos 服务地址 |
| `SERVER_PORT` | 9202 | oj-judge 服务端口 |
| `JUDGE_DOCKER_IMAGE` | eclipse-temurin:17-jdk-jammy | Docker 镜像 |

---

## 8. 判题服务特性

### 8.1 安全特性

1. **Docker 沙箱隔离**：代码在 Docker 容器中编译运行，与宿主机隔离
2. **只支持 Java**：当前版本仅支持 Java 语言提交
3. **输出规范化**：自动去空格和合并连续空格，支持格式容错
4. **超时限制**：可配置时间限制（默认 5000ms）
5. **内存限制**：可配置内存限制（默认 256MB）

### 8.2 性能特性

1. **临时文件清理**：使用 `tmp-judge` 目录存放临时文件
2. **Docker `--rm` 参数**：容器运行结束后自动清理
3. **超时保护**：编译和运行都有超时机制

### 8.3 已知限制

1. **仅支持 Java**：暂不支持 C/C++/Python 等其他语言
2. **单用例判题**：当前版本只支持单个测试用例
3. **文件上传限制**：需要配置 Docker 和共享卷
4. **期望输出必填**：`expectedResult` 不能为空

---

## 9. 故障排查

### 9.1 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 编译失败 | Docker 未启动、镜像���存在 | 检查 Docker 服务，确保镜像已 pull |
| 运行时错误 | 代码异常、内存超限 | 检查用户代码逻辑 |
| 答案错误 | 空格差异、格式问题 | 系统已做规范化处理 |
| 连接超时 | oj-judge 服务未启动 | 检查 oj-judge 服务状态 |
| 期望输出为空 | 数据库 expected_result 为 null | 确保题目已配置期望输出 |

### 9.2 日志查看

```bash
# oj-judge 服务日志
tail -f logs/oj-judge.log

# 查看判题请求日志（grep "JudgeRunService"）
grep "JudgeRunService" logs/oj-judge.log
```

---

## 10. API 端点汇总

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/friend/judge/submit` | 用户提交代码 |
| POST | `/judge/run` | oj-judge 内部判题接口 |
| GET | `/judge/ping` | oj-judge 健康检查 |

---

## 11. 扩展建议

### 11.1 支持多语言

1. 添加 `JudgeLanguageEnum` 枚举
2. 在 `RunAndOutput` 中增加语言对应的编译运行命令
3. 在 `ConnectCodeAndOutput` 中增加语言适配器

### 11.2 支持多测试用例

1. 扩展 `JudgeRunRequest` 支持测试用例数组
2. 在 `JudgeRunServiceImpl` 中遍历执行每个用例
3. 聚合多用例结果（全部通过才算通过）

### 11.3 添加内存/超时检测

1. 使用 `ProcessBuilder` 监控资源使用
2. 使用 Docker 资源限制参数：`--memory`、`--cpus`
3. 在 `RunAndOutput.run()` 中添加超时检测

---

*文档版本：1.0*
*最后更新：2026-04-07*
