---
name: code-generator
description: 按 e-print-admin 当前项目风格生成或重构代码。用于新增或修改后台模块、Controller、Service、MapStruct ModelMapper、Request、Param、Entity、Result、Dao、MyBatis XML、Thymeleaf 页面、首页、错误页、分页排序和批量操作；也用于维护本技能规则。
---

# e-print-admin 代码生成器

依据仓库中的现有实现生成或重构 `e-print-admin` 代码。仓库代码是事实来源；规范与代码冲突时，先核对相邻模块及近期变更，再按用户目标处理。

## 使用步骤

1. 阅读 [references/code-template.md](references/code-template.md)。
2. 检查目标模块及相邻模块，确认当前包结构、方法命名、路由、SQL 和页面写法。
3. 先确定本次变更涉及的分层文件，避免跨层复用不合适的模型。
4. 按 `Request → ModelMapper → Param → Service → Dao → XML` 实现数据流。
5. 同步修改 Java、Mapper XML、Thymeleaf、配置和测试中受影响的引用。
6. 执行与风险相称的编译、静态检查和页面验证。

## 强制规则

- Controller、Service、Dao 的业务方法尽量同名，统一按 `create → remove → modify → disable → enable → query → get/preview` 排列。
- Web 入参使用 `*Request`，持久层入参使用 `*Param`；禁止把 Request 直接传给 Dao 或 Mapper XML。
- 使用模块内 MapStruct `ModelMapper.INSTANCE.map(...)` 完成同名字段复制；禁止手写重复字段转换和 `toQueryParam` 一类方法。
- Service 可以依赖多个 Dao，但禁止 Service 调用 Service。
- 新增、修改 Param 默认继承 Entity；批量删除、启用、禁用 Param 默认继承 `IdsParam`。
- 分页请求、参数、结果统一使用 `common.model.page` 下的 `PageRequest`、`PageParam`、`PageResult`。
- 排序字段统一命名为 `sort`，排序白名单必须在 Mapper XML 的 `<sql id=order>` 中实现；禁止使用 `${sort}` 或其他用户输入的 SQL 字符串拼接。
- Mapper XML 的查询条件片段命名为 `where`，排序片段命名为 `order`。
- Controller 直接返回 `ResponseEntity.ok(Map.of(...))`；不要为一行返回值封装无意义的私有方法。
- 只有需要表单回显和安全返回地址的 Controller 才继承 `BaseController`。
- 业务校验、关联检查和唯一性检查放在 Service；Dao 与 Mapper XML 只负责持久化。
- 不新增 Manager 层，不在 Service 中保留排序 SQL 转换逻辑。
- 页面文案使用中文；Java 类型名、方法名、字段名保持英文。
- 除非用户明确要求维护本技能，否则不要修改 `.agents/skills/code-generator`。

## 验证要求

- Java 或 XML 变更：至少执行模块编译；MapStruct 映射变化优先执行 clean compile。
- 页面或脚本变更：检查模板引用和 JavaScript 语法；条件允许时运行页面验证。
- 技能自身变更：运行 `skill-creator/scripts/quick_validate.py`。
- 提交结果前执行 `git diff --check`，并确认没有误改用户已有内容。
