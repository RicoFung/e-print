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
- MinIO 与七牛模板独立维护，分别存储在 `E_PRINT_MINIO_*`、`E_PRINT_QINIU_*`
- HTML 模板上传、在线编辑和页面预览
- 模板文件可分别存储到 MinIO 和七牛对象存储
- 支持模板和类型的批量启用、禁用、删除

## 2. 运行与配置

本地运行要求：

- Java 21
- Oracle 数据库，包含 `E_PRINT_MINIO_*`、`E_PRINT_QINIU_*` 及对应序列
- 可访问的 MinIO bucket 和七牛 bucket

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
| `E_PRINT_QINIU_ACCESS_KEY` | 七牛 Access Key，仅通过部署 Secret 注入 |
| `E_PRINT_QINIU_SECRET_KEY` | 七牛 Secret Key，仅通过部署 Secret 注入 |
| `E_PRINT_QINIU_BUCKET` | 七牛 bucket，默认 `pos-uat` |
| `E_PRINT_QINIU_OBJECT_PREFIX` | 七牛对象根路径，默认 `e-print`；数据库 `OBJECT_NAME` 保存包含该前缀的完整对象 Key |
| `E_PRINT_QINIU_REGION` | 七牛区域，默认 `z2`（华南） |
| `E_PRINT_QINIU_S3_ENDPOINT` | 七牛 S3 Endpoint，华南默认 `https://s3.cn-south-1.qiniucs.com` |
| `E_PRINT_QINIU_S3_REGION` | 七牛 S3 Region，华南默认 `cn-south-1` |

数据库初始化脚本：

```text
../db/oracle/print_template.sql
```

该脚本一次性创建 MinIO、七牛的模板类型表、模板表、序列和索引，并初始化两套默认模板类型。`E_PRINT_MINIO_TEMPLATE`、`E_PRINT_QINIU_TEMPLATE` 不写入初始数据，模板需在管理后台创建。

## 3. 页面与数据

| 页面 | 用途 |
| --- | --- |
| `/e-print-admin/` | 后台首页 |
| `/e-print-admin/login` | 后台登录 |
| `/e-print-admin/minio/templates` | MinIO 模板管理 |
| `/e-print-admin/minio/template-types` | MinIO 类型管理 |
| `/e-print-admin/qiniu/templates` | 七牛模板管理 |
| `/e-print-admin/qiniu/template-types` | 七牛类型管理 |

两套模板类型均使用英文编码并独立维护，通过各自的 `TEMPLATE_TYPE_ID` 关联模板表。初始化脚本只预置下列类型，不预置模板元数据或模板文件。

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
GET /e-print-admin/{provider}/templates?templateTypeId=1&templateCode=01&status=1&pageSize=10
GET /e-print-admin/{provider}/templates/query?templateTypeId=1&templateCode=01&status=1&offset=0&limit=10
POST /e-print-admin/{provider}/templates/create
POST /e-print-admin/{provider}/templates/modify
POST /e-print-admin/{provider}/templates/enable
POST /e-print-admin/{provider}/templates/disable
POST /e-print-admin/{provider}/templates/remove
GET /e-print-admin/{provider}/templates/preview?id={id}
POST /e-print-admin/{provider}/templates/preview/render
```

```http
GET /e-print-admin/{provider}/template-types?keyword=sales&status=1&pageSize=10
GET /e-print-admin/{provider}/template-types/query?keyword=sales&status=1&offset=0&limit=10
POST /e-print-admin/{provider}/template-types/create
POST /e-print-admin/{provider}/template-types/modify
POST /e-print-admin/{provider}/template-types/enable
POST /e-print-admin/{provider}/template-types/disable
POST /e-print-admin/{provider}/template-types/remove
```

`{provider}` 取值为 `minio` 或 `qiniu`。
