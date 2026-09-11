# e-print-admin

`e-print-admin` 是 e-print 的后台管理服务，基于 Spring Boot MVC 和 Thymeleaf，用于维护 HTML 打印模板和模板类型。

## 目录

1. [核心能力](#1-核心能力)
2. [运行与配置](#2-运行与配置)
3. [页面与数据](#3-页面与数据)

## 1. 核心能力

- 管理员登录
- 模板类型新增、编辑、启用、禁用和删除
- 模板列表查询，支持按类型、编码、状态筛选
- 模板元数据维护，数据存储在 `E_PRINT_TEMPLATE`
- HTML 模板上传、在线编辑和页面预览
- 模板文件存储到 MinIO
- 支持模板和类型的批量启用、禁用、删除

## 2. 运行与配置

本地运行要求：

- Java 21
- Oracle 数据库，包含 `E_PRINT_TEMPLATE_TYPE`、`E_PRINT_TEMPLATE` 及对应序列
- 可访问的 MinIO bucket

```bash
cd e-print-admin
mvn -Ploc spring-boot:run
```

默认访问地址：

```text
http://localhost:8080/e-print-admin/
```

`/e-print-admin` 是应用的 context-path，Controller 和前端页面使用的应用内路由不再包含额外的 `/admin` 前缀。

默认登录账号：

```text
eprint / eprint123
```

常用命令：

```bash
mvn -Ploc -DskipTests compile
```

构建环境由 Maven Profile 决定，可选 `loc`、`uat`、`prod`；未指定时默认使用 `uat`。例如构建 UAT 可执行包：

```bash
mvn -Puat clean package -DskipTests
java -jar target/e-print-admin-exec.jar
```

主要环境变量：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_ADMIN_PORT` | HTTP 端口，默认 `8080` |
| `E_PRINT_ADMIN_USERNAME` | 管理员用户名 |
| `E_PRINT_ADMIN_PASSWORD` | 管理员密码 |
| `E_PRINT_DB_URL` | Oracle JDBC 地址 |
| `E_PRINT_DB_USERNAME` | Oracle 用户名 |
| `E_PRINT_DB_PASSWORD` | Oracle 密码 |
| `E_PRINT_MINIO_ENDPOINT` | MinIO 地址 |
| `E_PRINT_MINIO_ACCESS_KEY` | MinIO access key |
| `E_PRINT_MINIO_SECRET_KEY` | MinIO secret key |
| `E_PRINT_TEMPLATE_BUCKET` | 模板 bucket，默认 `e-print` |
| `E_PRINT_TEMPLATE_OBJECT_PREFIX` | 模板对象前缀，默认 `templates/print` |

数据库初始化脚本：

```text
../db/oracle/print_template.sql
```

## 3. 页面与数据

| 页面 | 用途 |
| --- | --- |
| `/e-print-admin/` | 后台首页 |
| `/e-print-admin/login` | 后台登录 |
| `/e-print-admin/templates` | 模板列表 |
| `/e-print-admin/templates/create` | 新增模板 |
| `/e-print-admin/templates/modify?id={id}` | 编辑模板 |
| `/e-print-admin/templates/preview?id={id}` | 预览模板 |
| `/e-print-admin/template-types` | 类型管理列表 |
| `/e-print-admin/template-types/create` | 新增类型 |
| `/e-print-admin/template-types/modify?id={id}` | 编辑类型 |

模板类型使用英文编码，后台页面展示中文名称。类型字典存储在 `E_PRINT_TEMPLATE_TYPE`，模板表 `E_PRINT_TEMPLATE` 通过 `TEMPLATE_TYPE_ID` 关联类型表。

默认模板类型：

| 编码 | 名称 |
| --- | --- |
| `sales_receipt` | 销售小票 |
| `sales_receipt_ed` | 销售小票 ed |
| `sales_receipt_ed2` | 销售小票 ed2 |
| `sales_receipt_o2o` | 销售小票 o2o |
| `shipping_label` | 物流面单 |
| `shipping_label_o2o` | 物流面单-o2o |
| `shipping_label_transfer_out` | 物流面单-横调出库 |
| `shipping_label_return_apply` | 物流面单-退货申请 |

同一模板类型下 `templateCode` 必须唯一；不同模板类型可以使用相同的 `templateCode`。每种模板类型的默认模板编码约定为 `01`。

常用页面接口：

```http
GET /e-print-admin/templates?templateTypeId=1&templateCode=01&status=1&pageSize=10
GET /e-print-admin/templates/query?templateTypeId=1&templateCode=01&status=1&offset=0&limit=10
POST /e-print-admin/templates/create
POST /e-print-admin/templates/modify
POST /e-print-admin/templates/enable
POST /e-print-admin/templates/disable
POST /e-print-admin/templates/remove
GET /e-print-admin/templates/preview?id={id}
POST /e-print-admin/templates/preview/render
```

```http
GET /e-print-admin/template-types?keyword=sales&status=1&pageSize=10
GET /e-print-admin/template-types/query?keyword=sales&status=1&offset=0&limit=10
POST /e-print-admin/template-types/create
POST /e-print-admin/template-types/modify
POST /e-print-admin/template-types/enable
POST /e-print-admin/template-types/disable
POST /e-print-admin/template-types/remove
```
