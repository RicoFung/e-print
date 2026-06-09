# e-print-client

`e-print-client` 是 Electron 本地打印桥接器，负责连接 `e-print-server`、接收 WebSocket 打印任务、下载 HTML 模板、渲染二维码和条码，并调用本地打印机完成打印。

## 核心能力

- 连接 `e-print-server`
- 支持 Basic 认证
- 接收 WebSocket 打印任务
- 下载服务端 HTML 打印模板
- Handlebars 渲染模板
- 生成二维码和条码
- 静默打印和非静默打印
- 打印结果回传
- 多语言和主题切换

## 本地运行

```bash
npm install
npm start
```

启动后会打开配置窗口，可配置：

- WebSocket 地址
- 客户端 ID
- 打印机
- 静默打印
- 主题
- 语言

## 配置

默认配置文件：

```text
e-print-client/config.json
```

客户端支持 `env + environments` 多环境配置。详细配置项见根目录 `README.md`。

常用环境变量：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_ENV` | 当前环境 |
| `E_PRINT_CLIENT_ID` | 客户端 ID |
| `E_PRINT_SERVER_URL` | WebSocket 地址 |
| `E_PRINT_BASIC_USERNAME` | Basic 用户名 |
| `E_PRINT_BASIC_PASSWORD` | Basic 密码 |
| `E_PRINT_PRINTER_NAME` | 默认打印机 |

## 模板约定

客户端从 WebSocket 任务中读取 `templateType` 和 `templateCode`，并按以下地址下载模板：

```http
GET /template/{templateCode}?templateType={templateType}
```

示例：

```http
GET /template/01?templateType=sales_receipt
```

本地缓存路径会按模板类型分目录，例如：

```text
{templateCacheDir}/sales_receipt/01.html
{templateCacheDir}/shipping_label/01.html
```

这样不同模板类型下相同的 `templateCode` 不会互相覆盖。

模板接口支持返回 `text/html`，也支持返回包含 `html`、`content` 或 `templateHtml` 字段的 JSON。

## WebSocket 任务报文

客户端接收的打印任务必须包含 `templateType`。缺少 `templateType`、`templateCode` 或 `taskId` 时，客户端会拒绝任务并上报失败。

任务消息示例：

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

打印结果回传地址：

```http
POST /task/{taskId}/result
Content-Type: application/json
```

请求体示例：

```json
{
  "status": "SUCCESS",
  "templateType": "sales_receipt",
  "templateCode": "01",
  "message": "打印完成"
}
```

模板示例：

```html
<section class="label">
  <h1>{{productName}}</h1>
  <p>{{sku}}</p>
  <img src="{{qr.qrText}}" alt="QR Code">
  <img src="{{barcode.barcodeText}}" alt="Barcode">
</section>
```

## 验证

```bash
npm test
```
