# e-print-server

`e-print-server` 是打印任务服务，基于 Spring Boot 和 niko-boot 搭建，负责接收业务系统打印请求，读取打印模板，维护客户端 WebSocket 连接，并将任务推送给指定的本地打印客户端。

## 目录

1. [核心能力](#1-核心能力)
2. [运行与配置](#2-运行与配置)
3. [接口协议](#3-接口协议)
4. [接口日志](#4-接口日志)

## 1. 核心能力

- 提供打印任务创建、查询和结果回传接口
- 维护本地打印客户端 WebSocket 连接，并按 `clientId` 推送任务
- 从相互独立的 MinIO／七牛 Oracle 表读取模板元数据
- 同时提供 MinIO、七牛 HTML 模板读取接口
- 支持 Basic 认证，`/e-print-server/health` 除外
- 支持 Graylog 日志输出和 SpringDoc 接口文档
- 统一记录 HTTP 接口的请求、响应、状态码和处理耗时

## 2. 运行与配置

本地运行要求：

- Java 21
- Oracle 数据库
- 可访问的 MinIO bucket 和七牛 bucket

```bash
cd e-print-server
mvn -Ploc spring-boot:run
```

默认访问地址：

```text
http://localhost:8080/e-print-server
```

默认 Basic 认证：

```text
eprint / eprint123
```

常用命令：

```bash
mvn -Ploc -DskipTests clean compile
```

构建环境由 Maven Profile 决定，可选 `loc`、`uat`、`prod`；未指定时默认使用 `uat`。例如构建 UAT 可执行包：

```bash
mvn -Puat clean package -DskipTests
java -jar target/e-print-server-exec.jar
```

配置文件：

```text
src/main/resources/application.yml
src/main/resources/application-loc.yml
src/main/resources/application-dev.yml
src/main/resources/application-uat.yml
src/main/resources/application-prod.yml
```

数据库初始化脚本：

```text
../db/oracle/print_template.sql
```

该脚本创建 MinIO、七牛两套模板表及配套数据库对象，并仅初始化模板类型字典；`E_PRINT_MINIO_TEMPLATE`、`E_PRINT_QINIU_TEMPLATE` 初始为空。

七牛模板相关环境变量：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_QINIU_ACCESS_KEY` | 七牛 Access Key，仅通过部署 Secret 注入 |
| `E_PRINT_QINIU_SECRET_KEY` | 七牛 Secret Key，仅通过部署 Secret 注入 |
| `E_PRINT_QINIU_BUCKET` | 七牛 bucket，默认 `pos-uat` |
| `E_PRINT_QINIU_S3_ENDPOINT` | 七牛 S3 Endpoint，华南默认 `https://s3.cn-south-1.qiniucs.com` |
| `E_PRINT_QINIU_S3_REGION` | 七牛 S3 Region，华南默认 `cn-south-1` |

## 3. 接口协议

除 `/e-print-server/health` 外，以下接口均需要 Basic 认证。

| 接口 | 说明 |
| --- | --- |
| `GET /e-print-server/health` | 健康检查 |
| `POST /e-print-server/task` | 创建打印任务 |
| `GET /e-print-server/task` | 查询任务列表 |
| `GET /e-print-server/task/{taskId}` | 查询单个任务 |
| `POST /e-print-server/task/{taskId}/result` | 回传打印结果 |
| `GET /e-print-server/minio/template/{templateCode}?templateType={templateType}` | 获取 MinIO HTML 模板 |
| `GET /e-print-server/qiniu/template/{templateCode}?templateType={templateType}` | 获取七牛 HTML 模板 |
| `WS /e-print-server/ws/print?clientId={clientId}` | 打印客户端连接入口 |

创建打印任务：

```http
POST /e-print-server/task
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
  "type": "print-task",
  "payload": {
    "taskId": "9d4d0c5f-7f4b-4f44-a1bb-2f0d2d4f1a01",
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
POST /e-print-server/task/{taskId}/result
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
打印任务只分发模板类型和编码，具体模板由客户端通过所配置的模板接口获取。

## 4. 接口日志

服务端默认通过一条日志同时记录一次 HTTP 请求及其最终响应，包括请求方法、路径、查询参数、客户端 IP、HTTP 状态码、处理耗时、请求体和响应体。WebSocket 升级请求不经过该日志记录。

日志会对 `password`、`accessToken`、`refreshToken`、`token`、`secret`、`authorization` 等 JSON 字段和查询参数脱敏；非文本正文只记录字节数。服务端会生成或沿用 `X-Request-Id`，写入日志 MDC 并通过同名响应头返回。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `E_PRINT_API_LOG_ENABLED` | `true` | 是否启用 HTTP 接口日志 |
| `E_PRINT_API_LOG_MAX_BODY_LENGTH` | `8192` | 请求体和响应体的最大日志字符数，超过后截断 |
