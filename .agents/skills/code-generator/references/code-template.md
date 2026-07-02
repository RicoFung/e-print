# e-print-admin 代码规范

本文描述仓库当前采用的代码风格。生成或重构代码前，应同时查看目标模块和相邻模块；现有代码与本文不一致时，以用户目标和当前有效实现为准，并同步修正规范。

## 目录

- [1. 技术与边界](#1-技术与边界)
- [2. 分层和包结构](#2-分层和包结构)
- [3. 方法命名与排列](#3-方法命名与排列)
- [4. 分页模型](#4-分页模型)
- [5. Request、Param、Entity 和 Result](#5-requestparamentity-和-result)
- [6. MapStruct ModelMapper](#6-mapstruct-modelmapper)
- [7. Controller](#7-controller)
- [8. Service](#8-service)
- [9. Dao](#9-dao)
- [10. MyBatis Mapper XML](#10-mybatis-mapper-xml)
- [11. 路由与页面](#11-路由与页面)
- [12. 首页和错误页](#12-首页和错误页)
- [13. Oracle 约束](#13-oracle-约束)
- [14. 检查清单](#14-检查清单)

## 1. 技术与边界

- Java 21、Spring Boot 4、Spring MVC、Spring Security、Thymeleaf、Jakarta Validation。
- MapStruct 负责模块内对象映射。
- MyBatis 与 Oracle 负责持久化。
- 管理页面使用 AdminLTE、Bootstrap Table、SweetAlert 等现有前端能力。
- 文件正文等大对象可存放在 MinIO，数据库保存元数据；保持两者职责分离。
- 不引入与项目现有结构重复的框架或抽象层。

## 2. 分层和包结构

常用目录如下：

```text
com.eprint.admin
├─ common
│  ├─ controller/BaseController.java
│  ├─ model/page/{PageRequest,PageParam,PageResult}.java
│  └─ support/PageSupport.java
├─ module/<module>
│  ├─ controller/*Controller.java
│  ├─ service/*Service.java
│  └─ model
│     ├─ ModelMapper.java
│     └─ request/*Request.java
└─ repository
   ├─ dao/*Dao.java
   ├─ mapper/*.xml
   └─ model
      ├─ entity/*.java
      ├─ param/*.java
      └─ result/*.java
```

标准调用链：

```text
Request → ModelMapper → Param → Service → Dao → Mapper XML
```

Controller 负责 HTTP、校验、页面模型和响应；Service 负责业务规则与事务；Dao 和 XML 负责持久化。

## 3. 方法命名与排列

Controller、Service、Dao 和 Mapper XML 的业务方法尽量同名：

```text
POST /disable
→ TemplateController.disable(...)
→ TemplateService.disable(...)
→ TemplateDao.disable(...)
→ <update id=disable>
```

类内按下列顺序排列：

1. `create`
2. `remove`
3. `modify`
4. `disable`
5. `enable`
6. `query`
7. `get`、`preview` 等单条查询或展示方法
8. 必要的私有辅助方法

单条和批量操作使用同名重载，通过 Spring MVC 的 `params = id`、`params = ids` 区分，不使用 `removeSelected`、`disableSelected` 等名称。

## 4. 分页模型

分页模型统一位于 `com.eprint.admin.common.model.page`：

- `PageRequest`：Web 查询字段，包含 `search`、`sort`、`order`、`offset`、`limit`、`page`、`pageSize`。
- `PageParam`：持久层分页字段，包含 `page`、`pageSize`、`sort`。
- `PageResult<T>`：响应字段，直接提供 `rows`、`total` 及分页辅助信息。

模块查询 Request 继承 `PageRequest`，查询 Param 继承 `PageParam`。同名字段由 MapStruct 自动复制。

分页大小统一通过 `PageSupport` 规范化：默认 10，最小 5，最大 100。不要在各 Service 重复实现边界逻辑。

## 5. Request、Param、Entity 和 Result

### 5.1 Request

- Request 只表示 Web 输入，放在模块的 `model.request` 包。
- 使用 Jakarta Validation 表达必填、长度、范围等输入约束。
- 状态字段可使用 `@NotNull`、`@Min(0)`、`@Max(1)`。
- 查询 Request 继承 `PageRequest`；新增、修改和批量操作 Request 各自独立定义。
- 不把 Entity 直接作为 Controller 入参。

常见类型：

```text
TemplateCreateRequest
TemplateRemoveRequest
TemplateModifyRequest
TemplateDisableRequest
TemplateEnableRequest
TemplateQueryRequest
```

### 5.2 Param

- Param 是 Dao 和 Mapper XML 的入参，放在 `repository.model.param`。
- `*CreateParam`、`*ModifyParam` 默认继承对应 Entity，避免重复声明持久化字段。
- 批量删除、启用、禁用 Param 默认继承公共 `IdsParam`；XML 遍历属性名固定为 `ids`。
- Request 不能直接传入 Dao 或 XML。

示例：

```java
public class TemplateCreateParam extends Template {
}

public class TemplateDisableParam extends IdsParam {
}
```

### 5.3 Entity 和 Result

- Entity 与表字段对应，不承担 Web 校验、页面回显或业务组合逻辑。
- Result 用于查询输出，可组合关联表显示字段。
- 不因为少量字段相同就混用 Request、Param、Entity、Result。

## 6. MapStruct ModelMapper

每个模块使用 MapStruct `ModelMapper` 统一映射：

```java
@Mapper
public interface ModelMapper {

    ModelMapper INSTANCE = Mappers.getMapper(ModelMapper.class);

    TemplateQueryParam map(TemplateQueryRequest source);

    TemplateCreateParam map(TemplateCreateRequest source);

    TemplateModifyParam map(TemplateModifyRequest source);
}
```

规则：

- 同名同类型字段交给 MapStruct 自动复制。
- 需要去除首尾空格的写入字段使用 `@Named(trim)` 等显式转换。
- 单个 `id` 与批量 `ids` 合并时，过滤空值、去除首尾空格并去重。
- Entity 回显到 ModifyRequest 时，可以忽略数据库之外的内容字段，再由 Service 从 MinIO 等存储读取。
- 禁止在 Service 手写重复的 `setXxx`，禁止创建 `toQueryParam`。
- 映射后若需业务默认值或合法性判断，应处理 Param，不修改 Controller 收到的 Request。

## 7. Controller

Controller 负责路由、参数绑定、`@Valid` 校验、对象映射、页面模型和 HTTP 响应。一个页面调用多个 Service 是允许的。

Controller 不负责 SQL 排序转换、业务唯一性和关联状态校验，不直接调用 Dao，也不手工复制 Request 字段到 Param。

简单结果直接返回，不封装一行私有方法：

```java
return ResponseEntity.ok(Map.of(modified, modified));
```

只有需要表单回显和返回列表地址的 Controller 才继承 `BaseController`。登录、健康检查、首页等无回显需求的 Controller 不继承。

`BaseController` 的返回地址必须限制在当前模块列表路径及其查询串，并拒绝空值、`//`、回车和换行，防止开放重定向和响应头注入。

编辑表单的下拉选项应包含当前记录已关联但后来被禁用的选项，避免回显丢失；新增表单通常只展示启用项。

## 8. Service

Service 负责业务默认值与规范化、唯一性与关联关系检查、状态转换、数据存在性检查、写操作事务，以及同一业务流程中的数据库与 MinIO 协调。

强制约束：

- 禁止 Service 调用 Service。
- 一个 Service 需要跨表检查时，直接注入所需 Dao。
- 写操作使用项目既有的 `transactionManagerMybatis` 事务配置。
- 不把 Web Request 下传到 Dao。
- 不在 Service 解析排序字段或生成 SQL 排序片段。
- 不为简单默认值和输入约束保留两套重复的 `normalize(CreateRequest)`、`normalize(ModifyRequest)`；优先使用 Bean Validation、MapStruct 转换和 Param 级共用逻辑。

## 9. Dao

Dao 继承项目公共 `BaseDao`，并注入：

```java
@Resource(name = sqlSessionTemplateMybatis)
private SqlSessionTemplate sqlSessionTemplate;
```

Dao 方法使用明确的业务 Param：

```java
public int create(TemplateCreateParam param) { ... }
public int remove(TemplateRemoveParam param) { ... }
public int modify(TemplateModifyParam param) { ... }
public int disable(TemplateDisableParam param) { ... }
public int enable(TemplateEnableParam param) { ... }
```

优先复用 `BaseDao`。如果公共删除方法不能接收业务 Param，可直接调用：

```java
return getSqlSession().delete(getStatementName(remove), param);
```

Dao 不做业务判断、不调用其他 Dao、不拼接 SQL。

## 10. MyBatis Mapper XML

### 10.1 查询条件

查询条件片段统一命名为 `where`，查询和计数复用同一片段。动态条件使用 `<where>`、`<if>` 和参数绑定，不重复维护 count 条件。

### 10.2 排序

排序片段统一命名为 `order`。排序白名单必须在 XML 中完成，不能在 Service 使用 `SortSupport.resolve`。

参照 `Template.xml`、`TemplateType.xml` 的当前实现：对 `sort.split(',', -1)` 使用 `<foreach>`，每项通过 `<choose>` 映射到写死的列名和方向，并限制可处理的排序项数量。

排序必须满足：

- 禁止直接使用用户输入替换排序字段。
- 无排序或非法排序时使用稳定的主键兜底。
- 多字段排序最后追加主键，保证分页稳定。
- 排序列、升降序和表达式都必须来自 XML 白名单。

### 10.3 写操作和批量操作

- `parameterType` 使用对应的 `*CreateParam`、`*RemoveParam`、`*ModifyParam`、`*DisableParam`、`*EnableParam`。
- 批量操作遍历 `ids`，不要遍历隐式的 `array`。
- SQL 的 `id` 与 Controller、Service、Dao 方法名一致。
- 参数值一律使用 `#{...}` 绑定。

### 10.4 Oracle 分页

沿用项目现有 `ROW_NUMBER()` 分页结构，查询数据和统计总数复用 `where`。排序先通过 `order` 白名单生成，再参与行号计算。

## 11. 路由与页面

后台资源通常采用：

```text
GET  /admin/<resources>
GET  /admin/<resources>/create
POST /admin/<resources>/create
POST /admin/<resources>/remove
GET  /admin/<resources>/modify
POST /admin/<resources>/modify
POST /admin/<resources>/disable
POST /admin/<resources>/enable
POST /admin/<resources>/query
GET  /admin/<resources>/preview
```

页面规则：

- 复用现有布局、菜单和 `admin.css`，不另建重复样式体系。
- 列表页沿用 Bootstrap Table 的分页、搜索、排序和批量操作模式。
- 表单错误时保留用户输入、选项数据和安全的 `returnUrl`。
- 成功后返回规范化后的列表地址，取消按钮也使用安全地址。
- 删除、启用、禁用等危险操作延续现有确认交互。
- 所有用户可见文案使用中文。

## 12. 首页和错误页

- 后台首页路由为 `/admin`，根路径和登录成功后的默认地址跳转到 `/admin`。
- 首页只展示快捷入口，不为装饰性统计查询 Service 或数据库。
- 导航菜单显示“首页”。
- 错误页至少覆盖 400、403、404、500 和通用错误模板，并复用公共结构。

Spring Boot 4 错误页配置使用：

```yaml
spring:
  web:
    error:
      whitelabel:
        enabled: false
      include-message: never
      include-stacktrace: never
```

不要继续使用已弃用的 `server.error` 下相关配置。

## 13. Oracle 约束

- 使用 Oracle 兼容语法与函数，避免 MySQL 或 PostgreSQL 专用语法。
- 分页和排序必须稳定。
- 字符串搜索、空值处理、时间函数和批量语句参照仓库已有 Mapper。
- 数据库字段名与别名保持大写下划线风格，Java 属性保持小驼峰。

## 14. 检查清单

### 分层和模型

- [ ] Request 没有直接进入 Dao 或 XML，Entity 没有直接作为 Controller 写接口入参。
- [ ] Controller、Service、Dao、XML 方法名尽量一致且排列一致。
- [ ] Service 没有调用其他 Service，业务校验位于 Service。
- [ ] 同名字段使用 MapStruct，没有手写重复转换或 `toQueryParam`。
- [ ] 新增、修改 Param 按需继承 Entity；批量 Param 继承 `IdsParam`。
- [ ] 分页统一使用 `PageRequest`、`PageParam`、`PageResult`。

### SQL 和页面

- [ ] 查询片段名为 `where`，排序片段名为 `order`。
- [ ] 排序白名单位于 XML，且没有直接字符串拼接排序字段。
- [ ] 非法排序和无排序都有稳定主键兜底，查询与计数复用过滤条件。
- [ ] 只有需要回显的 Controller 继承 `BaseController`，`returnUrl` 已限制路径。
- [ ] 简单 JSON 响应没有无意义的私有包装方法。
- [ ] 首页只保留快捷入口，错误页使用当前 Spring Boot 配置。

### 验证

- [ ] Java/XML 变更已编译；MapStruct 变化已按需 clean compile。
- [ ] 模板、脚本和路由引用已检查。
- [ ] `git diff --check` 通过。
- [ ] 技能规则变更已通过 `quick_validate.py`。
