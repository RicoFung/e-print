# e-print-admin

`e-print-admin` 是 e-print 的后台管理服务，基于 Spring Boot MVC 和 Thymeleaf，用于维护 HTML 打印模板。

## 功能

- 管理员登录。
- 模板列表查询，支持按 `templateType`、`templateCode`、`status` 筛选。
- 新增、编辑模板元数据，数据存储在 `E_PRINT_TEMPLATE`。
- 上传或在线编辑 HTML 模板内容。
- 模板文件存储到 MinIO。
- 在页面中预览模板。
- 支持启用、禁用和删除模板。
- 支持批量启用、批量禁用和批量删除。

## 模板类型

接口使用英文编码，后台页面展示中文名称。

| 编码 | 名称 |
| --- | --- |
| `sales_receipt` | 销售小票 |
| `sales_receipt_ed` | 销售小票-ed |
| `sales_receipt_ed2` | 销售小票-ed2 |
| `sales_receipt_o2o` | 销售小票-o2o |
| `shipping_label` | 物流面单 |
| `shipping_label_o2o` | 物流面单-o2o |
| `shipping_label_transfer_out` | 物流面单-横调出库 |
| `shipping_label_return_apply` | 物流面单-退货申请 |

同一模板类型下 `templateCode` 必须唯一；不同模板类型可以使用相同的 `templateCode`。每种模板类型的默认模板编码约定为 `01`。

## 本地运行

运行要求：

- Java 21
- Oracle 数据库，包含 `E_PRINT_TEMPLATE` 和 `SEQ_E_PRINT_TEMPLATE`
- 可访问的 MinIO bucket

```bash
cd e-print-admin
mvn spring-boot:run
```

默认本地访问地址：

```text
http://localhost:9091
```

默认本地登录账号：

```text
eprint / eprint123
```

## 配置

应用使用 Spring Profile，默认 profile 为 `loc`。

```powershell
$env:E_PRINT_ADMIN_PROFILE="uat"
java -jar e-print-admin.jar
```

主要环境变量：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_ADMIN_PROFILE` | 当前启用的 profile，默认 `loc` |
| `E_PRINT_ADMIN_PORT` | HTTP 端口，默认 `9091` |
| `E_PRINT_ADMIN_USERNAME` | 管理员用户名 |
| `E_PRINT_ADMIN_PASSWORD` | 管理员密码 |
| `E_PRINT_DB_URL` | Oracle JDBC 地址 |
| `E_PRINT_DB_USERNAME` | Oracle 用户名 |
| `E_PRINT_DB_PASSWORD` | Oracle 密码 |
| `E_PRINT_MINIO_ENDPOINT` | MinIO 地址 |
| `E_PRINT_MINIO_ACCESS_KEY` | MinIO access key |
| `E_PRINT_MINIO_SECRET_KEY` | MinIO secret key |
| `E_PRINT_TEMPLATE_BUCKET` | 默认模板 bucket，默认 `e-print` |
| `E_PRINT_TEMPLATE_OBJECT_PREFIX` | 默认模板对象前缀，默认 `templates/print` |

## 页面

| 页面 | 用途 |
| --- | --- |
| `/login` | 后台登录 |
| `/admin/templates` | 模板列表 |
| `/admin/templates/new` | 新增模板 |
| `/admin/templates/{id}/edit` | 编辑模板 |
| `/admin/templates/{id}/preview` | 预览模板 |

## 请求地址和表单字段

后台管理端是 Spring MVC + Thymeleaf 页面，不做前后端分离。列表表格的数据接口供页面内的 Bootstrap Table 调用。

### 模板列表页面

```http
GET /admin/templates?templateType=sales_receipt&templateCode=01&status=1&pageSize=10
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `templateType` | 否 | 模板类型编码 |
| `templateCode` | 否 | 模板编码，支持模糊查询 |
| `status` | 否 | 模板状态，`1` 启用，`0` 禁用 |
| `pageSize` | 否 | 页面默认每页条数 |

### 模板列表数据接口

```http
GET /admin/templates/data?templateType=sales_receipt&templateCode=01&status=1&offset=0&limit=10
```

返回体：

```json
{
  "total": 1,
  "rows": [
    {
      "id": "1",
      "templateType": "sales_receipt",
      "templateCode": "01",
      "bucketName": "e-print",
      "objectName": "templates/print/sales_receipt/01.html",
      "status": 1
    }
  ]
}
```

### 新增模板

```http
GET /admin/templates/new
```

```http
POST /admin/templates
Content-Type: application/x-www-form-urlencoded
```

表单字段：

```text
templateType=sales_receipt
templateCode=01
bucketName=e-print
objectName=templates/print/sales_receipt/01.html
status=1
content=<html>...</html>
```

### 编辑模板

```http
GET /admin/templates/{id}/edit
```

```http
POST /admin/templates/{id}
Content-Type: application/x-www-form-urlencoded
```

表单字段与新增模板一致。同一 `templateType` 下 `templateCode` 不允许重复，不同 `templateType` 可以使用相同 `templateCode`。

### 模板状态和删除

```http
POST /admin/templates/{id}/enable
POST /admin/templates/{id}/disable
POST /admin/templates/{id}/delete
```

批量操作：

```http
POST /admin/templates/enable
Content-Type: application/x-www-form-urlencoded

ids=1&ids=2
```

```http
POST /admin/templates/disable
Content-Type: application/x-www-form-urlencoded

ids=1&ids=2
```

```http
POST /admin/templates/delete
Content-Type: application/x-www-form-urlencoded

ids=1&ids=2
```

### 模板预览

```http
GET /admin/templates/{id}/preview
GET /admin/templates/{id}/preview/content
```

```http
POST /admin/templates/{id}/preview/render
Content-Type: application/x-www-form-urlencoded

sampleData={"productName":"示例商品","sku":"SKU-001"}
```

## 数据库初始化

初始化数据库时执行模板表脚本：

```text
e-print-server/src/main/resources/db/oracle/print_template.sql
```

该脚本会创建 `E_PRINT_TEMPLATE`、`SEQ_E_PRINT_TEMPLATE`、相关索引，并初始化 8 个模板类型的默认 `01` 模板元数据。

## 验证

```bash
mvn -DskipTests compile
```
