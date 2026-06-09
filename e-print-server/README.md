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
| `GET /template/{templateCode}?templateType=sales_receipt` | 获取 HTML 模板 |
| `WS /ws/print?clientId=CLIENT-001` | 打印客户端连接入口 |

## 请求示例

以下接口除 `/health` 外均需要 Basic 认证。

### 创建打印任务

```http
POST /task
Content-Type: application/json
Authorization: Basic ...
```

请求体：

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

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `clientId` | 是 | 目标打印客户端 ID，对应 WebSocket 连接参数 |
| `templateType` | 是 | 模板类型编码，例如 `sales_receipt` |
| `templateCode` | 是 | 模板编码 |
| `copies` | 否 | 打印份数，默认 `1`，最小值 `1` |
| `data` | 否 | 模板渲染数据 |

创建任务时会先查找启用状态的 `(templateType, templateCode)` 模板；如果找不到，则回退查找同类型的默认模板 `(templateType, 01)`。如果回退仍找不到，接口返回“Template not found”。任务下发给客户端时，`templateCode` 使用实际命中的模板编码，发生回退时为 `01`。

### 查询任务

```http
GET /task
Authorization: Basic ...
```

```http
GET /task/{taskId}
Authorization: Basic ...
```

任务数据示例：

```json
{
  "taskId": "9d4d0c5f7f4b4f44a1bb2f0d2d4f1a01",
  "clientId": "CLIENT-001",
  "templateType": "sales_receipt",
  "templateCode": "01",
  "copies": 1,
  "data": {
    "productName": "示例商品",
    "sku": "SKU-001"
  },
  "status": "PENDING",
  "resultMessage": null,
  "createdAt": "2026-06-08T07:00:00Z",
  "updatedAt": "2026-06-08T07:00:00Z"
}
```

### 回传打印结果

```http
POST /task/{taskId}/result
Content-Type: application/json
Authorization: Basic ...
```

请求体：

```json
{
  "status": "SUCCESS",
  "templateType": "sales_receipt",
  "templateCode": "01",
  "message": "打印完成"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 是 | 打印结果状态，例如 `SUCCESS` 或 `FAILED` |
| `templateType` | 否 | 客户端实际使用的模板类型 |
| `templateCode` | 否 | 客户端实际使用的模板编码 |
| `message` | 否 | 结果说明或失败原因 |

### 获取模板

```http
GET /template/{templateCode}?templateType={templateType}
Authorization: Basic ...
```

示例：

```http
GET /template/02?templateType=sales_receipt
Authorization: Basic ...
```

模板查询同样执行默认模板回退逻辑：找不到 `sales_receipt/02` 时，会尝试返回 `sales_receipt/01`。

返回内容可能是 `text/html`，也可能是包含模板内容的 JSON 数据，客户端兼容读取 `html`、`content` 或 `templateHtml` 字段。

### WebSocket 客户端连接

```http
WS /ws/print?clientId=CLIENT-001
Authorization: Basic ...
```

服务端推送给客户端的任务消息中会包含 `templateType` 和实际命中的 `templateCode`：

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

## 数据库初始化

初始化 Oracle 模板表时执行：

```text
src/main/resources/db/oracle/print_template.sql
```

该脚本会创建 `E_PRINT_TEMPLATE`、`SEQ_E_PRINT_TEMPLATE`、相关索引，并初始化 8 个模板类型的默认 `01` 模板元数据。

## 验证

```bash
mvn -DskipTests clean compile
```
