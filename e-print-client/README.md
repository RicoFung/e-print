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

也可以通过环境变量指定配置：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_CONFIG_PATH` | 配置文件路径 |
| `E_PRINT_ENV` | 当前环境 |
| `E_PRINT_CLIENT_ID` | 客户端 ID |
| `E_PRINT_SERVER_URL` | WebSocket 地址 |
| `E_PRINT_BASIC_USERNAME` | Basic 用户名 |
| `E_PRINT_BASIC_PASSWORD` | Basic 密码 |
| `E_PRINT_PRINTER_NAME` | 默认打印机 |

## 3. 任务协议

客户端从 WebSocket 任务中读取 `templateType`、`templateCode` 和 `taskId`。缺少任一关键字段时，客户端会拒绝任务并上报失败。

模板下载接口：

```http
GET /template/{templateCode}?templateType={templateType}
```

打印任务示例：

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
      "sku": "SKU-001",
      "qr": {
        "qrText": "https://example.com/item/SKU-001"
      },
      "barcode": {
        "barcodeText": "SKU-001"
      }
    }
  }
}
```

打印结果回传：

```http
POST /task/{taskId}/result
Content-Type: application/json
```

```json
{
  "status": "SUCCESS",
  "templateType": "sales_receipt",
  "templateCode": "01",
  "message": "打印完成"
}
```
