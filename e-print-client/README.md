# E-Print

`E-Print`（项目目录 `e-print-client`）是 Electron 本地打印桥接器，负责连接 `e-print-server`，接收 WebSocket 打印任务，下载并渲染 HTML 模板，生成二维码和条码，并调用本地打印机完成打印。

## 目录

1. [核心能力](#1-核心能力)
2. [运行与配置](#2-运行与配置)
3. [任务协议](#3-任务协议)

## 1. 核心能力

- 连接 `e-print-server`，接收 WebSocket 打印任务
- 支持服务地址、Basic 认证、客户端 ID、打印机、静默打印等配置
- 优先通过 Electron 枚举打印机，返回为空或失败时自动回退到 Windows 打印机列表
- UAT 与 PROD 使用独立应用身份和配置目录，可在同一台电脑上共存
- UAT 与 PROD 的 Basic 凭据在打包时按环境内置，运行时无需用户输入
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

运行测试：

```bash
npm test
```

### 2.1 Windows 打包

生成带安装向导和进度条的 NSIS 安装包：

```powershell
# 单独生成 UAT；PROD 同理改用 npm run dist:win:prod
$env:E_PRINT_BASIC_USERNAME = 'uat-user'
$env:E_PRINT_BASIC_PASSWORD = 'uat-password'
npm run dist:win:uat
Remove-Item Env:E_PRINT_BASIC_USERNAME, Env:E_PRINT_BASIC_PASSWORD

# 一次生成 UAT 和 PROD，分别传入两套凭据
$env:E_PRINT_UAT_BASIC_USERNAME = 'uat-user'
$env:E_PRINT_UAT_BASIC_PASSWORD = 'uat-password'
$env:E_PRINT_PROD_BASIC_USERNAME = 'prod-user'
$env:E_PRINT_PROD_BASIC_PASSWORD = 'prod-password'
npm run dist:win:all
Remove-Item Env:E_PRINT_UAT_BASIC_USERNAME, Env:E_PRINT_UAT_BASIC_PASSWORD,
  Env:E_PRINT_PROD_BASIC_USERNAME, Env:E_PRINT_PROD_BASIC_PASSWORD
```

便携版和解包目录命令使用相同的环境变量。环境专用变量优先于通用的
`E_PRINT_BASIC_USERNAME`、`E_PRINT_BASIC_PASSWORD`。

安装包使用交互式安装向导，允许选择安装目录，并创建桌面和开始菜单快捷方式。所有打包文件名使用小写：
卸载时会先结束对应环境的后台进程，并清理该环境当前版本及旧版本遗留的开机自启项；卸载 UAT 不会结束 PROD，反之亦然。

```text
UAT:  dist/uat/e-print-uat-setup-{version}.exe
PROD: dist/prod/e-print-setup-{version}.exe
```

如需便携版，可使用：

```bash
npm run dist:win:uat:portable
npm run dist:win:prod:portable
```

生成解包目录，用于本地检查打包内容：

```bash
npm run pack:win:uat
npm run pack:win:prod
```

兼容命令 `npm run dist:win` 默认构建 PROD NSIS 安装包；`npm run dist:win:portable` 默认构建
PROD 便携版；`npm run pack:win` 和 `npm run pack:win:unsigned` 默认生成 PROD 解包目录。

环境与产物：

| 项目 | UAT | PROD |
| --- | --- | --- |
| App ID | `com.eprint.client.uat` | `com.eprint.client.prod` |
| 应用名 | `E-Print-UAT` | `E-Print` |
| 进程名 | `E-Print-UAT.exe` | `E-Print.exe` |
| 开机启动项 | `E-Print-UAT` | `E-Print` |
| NSIS 安装包 | `dist/uat/e-print-uat-setup-{version}.exe` | `dist/prod/e-print-setup-{version}.exe` |
| 便携包 | `dist/uat/e-print-uat-portable-{version}.exe` | `dist/prod/e-print-portable-{version}.exe` |
| 解包目录 | `dist/uat/win-unpacked` | `dist/prod/win-unpacked` |
| 用户配置目录 | `%APPDATA%/E-Print-UAT` | `%APPDATA%/E-Print` |

两套应用的用户配置、模板缓存、单实例锁和开机自启均相互独立，因此可以同时运行。
客户端通过开机启动项在用户登录后常驻托盘，不注册为 Windows 服务。
安装包内不携带运行时 `config.json`。打包时通过环境变量传入 Basic 凭据，打包器只将当前目标环境的凭据
内置到对应安装包；缺少用户名或密码时会直接终止构建，避免生成无法连接的安装包。

### 2.2 首次配置

首次启动时需要设置：

- 服务地址
- 客户端 ID
- 打印机和打印模式

“服务地址”只填写公共前缀。例如：

```text
wss://apiuat.moco.com/eprint/v1
```

客户端会自动生成相关接口地址：

```text
WebSocket:    wss://apiuat.moco.com/eprint/v1/ws/print
Template API: https://apiuat.moco.com/eprint/v1/{qiniu|minio}/template
Result API:   https://apiuat.moco.com/eprint/v1/task/{taskId}/result
```

为兼容旧配置，输入完整的 `/ws/print` 地址也可以，保存时会自动标准化。服务地址未填写时，
客户端保持未连接状态，不会创建 WebSocket 连接。

运行时配置文件分别保存在：

```text
UAT:  %APPDATA%/E-Print-UAT/config.json
PROD: %APPDATA%/E-Print/config.json
```

用户名和密码不在界面显示，也不会写入运行时配置文件。修改某个环境的 Basic 凭据后，需要重新生成并安装该环境的安装包。

首次安装以及从旧版本升级后，打印模式默认为“预览”；用户仍可在界面中手动切换为“静默”。

### 2.3 环境变量

开发和受控部署场景也可以通过环境变量指定配置：

| 变量 | 说明 |
| --- | --- |
| `E_PRINT_CONFIG_PATH` | 配置文件路径 |
| `E_PRINT_ENV` | 当前环境，主要用于本地开发 |
| `E_PRINT_CLIENT_ID` | 客户端 ID |
| `E_PRINT_SERVER_URL` | 服务基础地址或完整 WebSocket 地址 |
| `E_PRINT_TEMPLATE_SOURCE` | 模板源，可选 `qiniu`、`minio`，默认 `qiniu` |
| `E_PRINT_TEMPLATE_BASE_URL` | 模板 HTTP API 地址；设置 `E_PRINT_SERVER_URL` 时未显式配置则自动推导 |
| `E_PRINT_BASIC_USERNAME` | Basic 用户名，仅用于本地开发；安装包使用打包时内置值 |
| `E_PRINT_BASIC_PASSWORD` | Basic 密码，仅用于本地开发；安装包使用打包时内置值 |
| `E_PRINT_PRINTER_NAME` | 默认打印机 |

模板源默认使用 `qiniu`，界面不显示该配置项。如需切换为 `minio`，可通过已有配置文件或
`E_PRINT_TEMPLATE_SOURCE` 环境变量设置，模板下载地址会随之自动切换。

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
