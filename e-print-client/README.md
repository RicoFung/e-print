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

模板接口支持返回 `text/html`，也支持返回包含 `html`、`content` 或 `templateHtml` 字段的 JSON。

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
