# e-print-server

`e-print-server` 是打印任务服务，基于 Spring Boot 和 niko-boot 搭建，负责接收业务系统打印请求，读取打印模板，维护客户端 WebSocket 连接，并将任务推送给指定的本地打印客户端。

## 目录

1. [核心能力](#1-核心能力)
2. [运行与配置](#2-运行与配置)
3. [接口协议](#3-接口协议)

## 1. 核心能力

- 提供打印任务创建、查询和结果回传接口
- 维护本地打印客户端 WebSocket 连接，并按 `clientId` 推送任务
- 从 Oracle 读取打印模板元数据
- 从 MinIO 读取 HTML 打印模板
- 支持 Basic 认证，`/health` 除外
- 支持 Graylog 日志输出和 SpringDoc 接口文档

## 2. 运行与配置

本地运行要求：

- Java 21
- Oracle 数据库
- 可访问的 MinIO bucket

```bash
cd e-print-server
mvn spring-boot:run
```

默认端口：

```text
9090
```

默认 Basic 认证：

```text
eprint / eprint123
```

常用命令：

```bash
mvn -DskipTests clean compile
```

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

数据库初始化脚本：

```text
../db/oracle/print_template.sql
```

## 3. 接口协议

除 `/health` 外，以下接口均需要 Basic 认证。

| 接口 | 说明 |
| --- | --- |
| `GET /health` | 健康检查 |
| `POST /task` | 创建打印任务 |
| `GET /task` | 查询任务列表 |
| `GET /task/{taskId}` | 查询单个任务 |
| `POST /task/{taskId}/result` | 回传打印结果 |
| `GET /template/{templateCode}?templateType={templateType}` | 获取 HTML 模板 |
| `WS /ws/print?clientId={clientId}` | 打印客户端连接入口 |

创建打印任务：

```http
POST /task
Content-Type: application/json
Authorization: Basic ...
```

```json
{
  "clientId": "CLIENT-001",
  "templateType": "sales_receipt",
  "templateCode": "02",
  "copies": 1,
  "data": {
    "productName": "示例商品",
    "sku": "SKU-001",
    "price": "19.90"
  }
}
```

任务推送消息：

```json
{
  "type": "PRINT_TASK",
  "data": {
    "taskId": "9d4d0c5f7f4b4f44a1bb2f0d2d4f1a01",
    "clientId": "CLIENT-001",
    "templateType": "sales_receipt",
    "templateCode": "01",
    "copies": 1,
    "data": {
      "productName": "示例商品",
      "sku": "SKU-001"
    }
  }
}
```

打印结果回传：

```http
POST /task/{taskId}/result
Content-Type: application/json
Authorization: Basic ...
```

```json
{
  "status": "SUCCESS",
  "templateType": "sales_receipt",
  "templateCode": "01",
  "message": "打印完成"
}
```

模板查询会按 `(templateType, templateCode)` 查找启用模板；找不到时回退到同类型默认模板 `(templateType, 01)`。
