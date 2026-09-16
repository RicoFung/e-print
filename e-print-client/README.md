# e-print-client

`e-print-client` 是 Electron 本地打印桥接器，负责连接 `e-print-server`，接收 WebSocket 打印任务，下载并渲染 HTML 模板，生成二维码和条码，并调用本地打印机完成打印。

## 目录

1. [核心能力](#1-核心能力)
2. [运行与配置](#2-运行与配置)
3. [任务协议](#3-任务协议)

## 1. 核心能力

- 连接 `e-print-server`，接收 WebSocket 打印任务
- 支持 Basic 认证、客户端 ID、打印机、静默打印等配置
- 下载服务端 HTML 模板，并使用 Handlebars 渲染打印内容
- 生成二维码和条码，支持打印结果回传
- 支持多语言和主题切换

## 2. 运行与配置

本地运行要求：

- Node.js 18+
- npm 9+

```bash
npm install
npm start
```

常用命令：

```bash
npm test
npm run dist:win
npm run pack:win:unsigned
```

配置文件：

```text
e-print-client/config.json
%APPDATA%/e-print-client/config.json
```

默认本地服务地址已对齐 `e-print-server` 的 `8080` 端口和 `/e-print-server` Context Path：

```text
WebSocket: ws://localhost:8080/e-print-server/ws/print
Template API: http://localhost:8080/e-print-server/qiniu/template
```

配置文件中的环境名称统一使用 `loc`、`dev`、`uat`、`prod`。

也可以通过环境变量指定配置：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_CONFIG_PATH` | 配置文件路径 |
| `E_PRINT_ENV` | 当前环境 |
| `E_PRINT_CLIENT_ID` | 客户端 ID |
| `E_PRINT_SERVER_URL` | WebSocket 地址 |
| `E_PRINT_TEMPLATE_SOURCE` | 模板源，可选 `qiniu`、`minio`，默认 `qiniu` |
| `E_PRINT_TEMPLATE_BASE_URL` | 模板 HTTP API 地址；设置 `E_PRINT_SERVER_URL` 时未显式配置则自动推导 |
| `E_PRINT_BASIC_USERNAME` | Basic 用户名 |
| `E_PRINT_BASIC_PASSWORD` | Basic 密码 |
| `E_PRINT_PRINTER_NAME` | 默认打印机 |

客户端可在“模板源”下拉框中选择 `qiniu` 或 `minio`，默认使用 `qiniu`。模板下载地址会随选择自动切换为
`/e-print-server/qiniu/template` 或 `/e-print-server/minio/template`。

## 3. 任务协议

客户端从 WebSocket 任务中读取 `clientId`、`templateType`、`templateCode` 和 `taskId`。其中 `clientId`、`templateType`、`templateCode` 缺失时客户端会拒绝任务；`taskId` 用于打印结果回传，应由服务端创建任务时提供。

模板下载接口：

```http
GET /e-print-server/{provider}/template/{templateCode}?templateType={templateType}
Authorization: Basic ...
```

打印任务示例：

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
      "sku": "SKU-001",
      "codes": {
        "productBarcode": {
          "codeType": "barcode",
          "value": "SKU-001"
        },
        "productQr": {
          "codeType": "qr",
          "value": "https://example.com/item/SKU-001"
        }
      }
    }
  }
}
```

条码和二维码对象使用业务名称，可以放在 `data` 的任意嵌套位置。`codeType` 只支持 `barcode` 和 `qr`，
客户端会根据 `value` 生成 `dataUrl`，模板通过 `{{codes.productBarcode.dataUrl}}` 等路径引用。

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
