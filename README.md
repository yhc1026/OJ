# bite-oj（Spring Cloud 微服务骨架）

按课件《19. 项目起步-后端工程创建》生成的多模块工程结构：

```text
bite-oj
├─ oj-api                  服务间调用的 api（占位）
├─ oj-common               公共包聚合
│  └─ oj-common-redis
├─ oj-gateway              网关
└─ oj-modules              微服务聚合
   ├─ oj-system
   ├─ oj-judge
   ├─ oj-job
   └─ oj-friend
```

## 环境要求

- JDK 17
- Maven 3.8+

## 构建

```bash
mvn -DskipTests package
```

## 启动

每个服务的配置放在各自的 `src/main/resources/bootstrap.yml`。

说明：
- 本仓库当前是“项目起步骨架”，**未接入**注册中心/配置中心/数据库等（课件后续章节再逐步补齐）
- 每个微服务都提供了一个最小化 `/ping` 接口，用于快速验证服务可启动、端口不冲突

```bash
# 网关（9000）
mvn -pl oj-gateway spring-boot:run

# 微服务
mvn -pl oj-modules/oj-system spring-boot:run   # 9201
mvn -pl oj-modules/oj-judge spring-boot:run    # 9202
mvn -pl oj-modules/oj-job spring-boot:run      # 9203
mvn -pl oj-modules/oj-friend spring-boot:run   # 9204
```

启动后可访问：

- `http://localhost:9201/ping`
- `http://localhost:9202/ping`
- `http://localhost:9203/ping`
- `http://localhost:9204/ping`





