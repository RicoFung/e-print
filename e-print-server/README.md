# e-print-server

`e-print-server` 是打印任务服务，基于 Spring Boot 和 niko-boot 搭建，负责接收业务系统打印请求、读取打印模板、维护客户端 WebSocket 连接，并将任务推送给指定的本地打印客户端。

## 核心能力

- Basic 认证，`/health` 除外
- 打印任务创建、查询和结果回传
- WebSocket 客户端注册和任务推送
- Oracle + MyBatis 读取打印模板元数据
- MinIO 读取 HTML 打印模板
- Graylog 日志输出
- SpringDoc 接口文档

## 接口

| 接口 | 说明 |
| --- | --- |
| `GET /health` | 健康检查，不需要 Basic 认证 |
| `POST /task` | 创建打印任务 |
| `GET /task` | 查询任务列表 |
| `GET /task/{taskId}` | 查询单个任务 |
| `POST /task/{taskId}/result` | HTTP 回传打印结果 |
| `GET /template/{templateCode}` | 获取 HTML 模板 |
| `WS /ws/print?clientId=CLIENT-001` | 打印客户端连接入口 |

## 本地运行

要求 Java 21。

```bash
mvn spring-boot:run
```

默认端口：

```text
9090
```

本地默认 Basic 认证：

```text
eprint / eprint123
```

## 多环境

服务端使用 Spring Profile：

```powershell
$env:E_PRINT_PROFILE="uat"
java -jar e-print-server.jar
```

配置文件：

```text
src/main/resources/application.yml
src/main/resources/application-loc.yml
src/main/resources/application-dev.yml
src/main/resources/application-uat.yml
src/main/resources/application-prd.yml
```

详细配置项见根目录 `README.md`。

## 验证

```bash
mvn -DskipTests clean compile
```
