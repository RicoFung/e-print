# e-print-client

本地打印客户端，负责连接 `e-print-server`、接收 WebSocket 打印任务、下载模板、渲染二维码/条码，并通过 Electron 调用本地打印。

## 运行

```bash
npm install
npm start
```

启动后会打开配置窗口，可以修改 WebSocket 地址、保存配置、查看连接状态并手动重连。也可以选择本地打印机、切换静默打印，并打印测试页验证打印链路。

## 配置

默认配置文件位于项目内：

```text
e-print-client/config.json
```

配置窗口保存后会写入该文件。环境变量仍可覆盖配置：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `E_PRINT_CLIENT_ID` | 客户端 ID | `CLIENT-001` |
| `E_PRINT_SERVER_URL` | WebSocket 地址 | `ws://localhost:9090/ws/print` |
| `E_PRINT_TEMPLATE_BASE_URL` | 模板接口基础地址 | `http://localhost:9090/api/templates` |
| `E_PRINT_PRINTER_NAME` | 本地打印机名称 | 空，使用系统默认打印机 |

UI 中的打印机配置会保存到 `printerName` 和 `silent`：

```json
{
  "printerName": "",
  "silent": true
}
```

`printerName` 为空时使用系统默认打印机。测试页会使用当前选择的打印机和静默打印设置。

在 UI 中修改 WebSocket 地址时，模板接口地址会按同一主机自动推导：

```text
ws://localhost:9090/ws/print -> http://localhost:9090/api/templates
wss://print.example.com/ws/print -> https://print.example.com/api/templates
```

## 验证

```bash
npm test
```

当前单测不依赖真实打印机，也不依赖已安装的 Electron。测试覆盖：

- 配置加载、旧端口迁移和模板接口地址推导
- 打印机配置保存和测试页打印入口
- 打印任务校验和 WebSocket 消息解析
- 模板渲染和二维码/条码资源注入
- 打印成功/失败结果回传
- WebSocket 连接状态变化

## WebSocket 协议

客户端连接后发送注册消息：

```json
{
  "type": "client-register",
  "clientId": "CLIENT-001"
}
```

服务端可发送打印任务，或使用包装格式：

```json
{
  "type": "print-task",
  "payload": {
    "taskId": "TASK-001",
    "clientId": "CLIENT-001",
    "templateCode": "product-label",
    "copies": 1,
    "data": {
      "productName": "MacBook Pro",
      "sku": "MBP-001",
      "price": "19999",
      "qrText": "https://example.com/p/MBP-001",
      "barcodeText": "MBP-001"
    }
  }
}
```

打印后客户端回传：

```json
{
  "type": "print-result",
  "taskId": "TASK-001",
  "clientId": "CLIENT-001",
  "templateCode": "product-label",
  "status": "success",
  "printedAt": "2026-05-12T08:00:00.000Z"
}
```

## 模板约定

模板接口支持返回 `text/html`，或返回 JSON 且包含 `html`、`content`、`templateHtml` 任一字段。

Handlebars 模板可以使用任务数据，也可以引用自动生成的图片地址：

```html
<section class="label">
  <h1>{{productName}}</h1>
  <p>{{sku}}</p>
  <img src="{{qr.qrText}}" />
  <img src="{{barcode.barcodeText}}" />
</section>
```
