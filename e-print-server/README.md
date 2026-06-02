# e-print-server

基于 `niko-boot` 的最小打印服务，当前版本不连接数据库，打印任务保存在内存中。

## 接口

- `GET /api/health`：健康检查
- `POST /api/print/tasks`：创建打印任务
- `GET /api/print/tasks`：查询内存中的打印任务
- `GET /api/print/tasks/{taskId}`：查询单个任务
- `POST /api/print/tasks/{taskId}/result`：客户端回传打印结果
- `GET /api/templates/{templateCode}`：获取临时 HTML 模板
- `WS /ws/print?clientId=CLIENT-001`：打印客户端连接入口

## 创建打印任务示例

```json
{
  "clientId": "CLIENT-001",
  "templateCode": "product-label",
  "copies": 1,
  "data": {
    "productName": "MacBook Pro",
    "sku": "MBP-001",
    "price": "19999",
    "qrText": "https://example.com/p/MBP-001"
  }
}
```

## 运行

`niko-boot-parent` 当前要求 Java 21。使用 Maven 启动：

```bash
mvn spring-boot:run
```
