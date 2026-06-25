# e-print-admin 代码生成模板

本文档是 `e-print-admin` 新增或重构后台管理模块时的唯一标准模板。生成代码时直接按本文档落地。

## 1. 项目边界

仅适用于 `e-print-admin`：

- 页面模块：`src/main/java/com/eprint/admin/module/**`
- 持久化层：`src/main/java/com/eprint/admin/repository/**`
- Thymeleaf 页面：`src/main/resources/templates/**`
- 静态资源：`src/main/resources/static/**`
- Oracle 脚本：仓库根目录 `db/oracle/**`

不生成 `e-print-server` API 代码，不生成 REST-only 后台模块。

## 2. 技术栈

- Java 21。
- Spring Boot 4.x。
- Spring MVC + Thymeleaf。
- Bean Validation 使用 `jakarta.validation.*`。
- Dao 继承 `com.niko.boot.dao.BaseDao`，并注入 MyBatis `SqlSession`。
- 数据库为 Oracle。
- 列表页使用 Bootstrap Table。
- 提示弹窗使用现有 SweetAlert 风格。

禁止在新代码中引入以下非本项目模板能力：

- Swagger/OpenAPI 注解。
- `NikoResult`。
- `BaseRestController`。
- MapStruct。
- Manager 层。
- 缓存注解。
- `Admin` 类名前缀或中缀。

## 3. 标准分层

```text
e-print-admin/src/main/java/com/eprint/admin/
├── module/{domain}/
│   ├── controller/{Module}Controller.java
│   ├── service/{Module}Service.java
│   └── model/request/
│       ├── {Module}QueryRequest.java
│       ├── {Module}CreateRequest.java
│       ├── {Module}ModifyRequest.java
│       ├── {Module}RemoveRequest.java
│       └── {Module}PreviewRequest.java
└── repository/
    ├── dao/{Module}Dao.java
    ├── mapper/{Module}.xml
    └── model/
        ├── entity/{Module}.java
        ├── param/{Module}QueryParam.java
        └── result/{Module}Result.java
```

规则：

- Controller 只处理路由、请求绑定、页面模型和响应封装。
- Service 处理业务校验、事务、转换、排序白名单和关联检查。
- Dao 继承 `BaseDao`，只提供 SqlSession、namespace 和必要的类型化业务方法。
- Mapper XML 只写 SQL。
- Web Request 不作为 MyBatis `parameterType`。
- 不创建 `{Module}Form`。
- Result 承载列表、详情和关联查询返回值；Entity 只表达可写入的表字段。

## 4. 路由、方法、页面

Controller 根路径使用复数资源名：

```java
@Controller
@RequestMapping("/admin/templates")
public class TemplateController {
}
```

页面动作必须一一对应：

| 动作 | HTTP | 路径 | Controller 方法 | 页面 |
| --- | --- | --- | --- | --- |
| 查询页 | GET | `/` | `query(...)` | `template/query.html` |
| 新增页 | GET | `/create` | `create(...)` | `template/create.html` |
| 新增提交 | POST | `/create` | `create(...)` | redirect |
| 编辑页 | GET | `/modify` | `modify(...)` | `template/modify.html` |
| 编辑提交 | POST | `/modify` | `modify(...)` | redirect |
| 预览页 | GET | `/preview` | `preview(...)` | `template/preview.html` |

数据和操作接口：

| 动作 | HTTP | 路径 | 方法 |
| --- | --- | --- | --- |
| 表格数据 | GET | `/query` | `query(...)` |
| 启用 | POST | `/enable` | `enable(...)` |
| 禁用 | POST | `/disable` | `disable(...)` |
| 删除 | POST | `/remove` | `remove(...)` |
| 预览渲染 | POST | `/preview/render` | `renderPreview(...)` |

## 5. Request 模板

每个请求动作独立 Request，不复用、不继承。

```text
{Module}QueryRequest
{Module}CreateRequest
{Module}ModifyRequest
{Module}RemoveRequest
{Module}PreviewRequest
```

位置：

```text
com.eprint.admin.module.{domain}.model.request
```

示例：

```java
package com.eprint.admin.module.template.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TemplateCreateRequest {

    @NotBlank(message = "类型不能为空")
    private String templateTypeId;

    @NotBlank(message = "编码不能为空")
    private String templateCode;

    @NotBlank(message = "Bucket 不能为空")
    private String bucketName;

    @NotBlank(message = "Object 不能为空")
    private String objectName;

    @NotNull(message = "状态不能为空")
    private Integer status;

    // getters and setters
}
```

分页查询 Request 示例：

```java
public class TemplateQueryRequest {

    private String templateTypeId;
    private String templateCode;
    private Integer status;
    private String search;
    private String sort;
    private Integer offset = 0;
    private Integer limit = 10;

    public int page() {
        int size = limit();
        return offset == null || offset < 0 ? 1 : (offset / size) + 1;
    }

    public int limit() {
        return limit == null || limit < 1 ? 10 : limit;
    }

    public String queryTemplateCode() {
        return templateCode == null || templateCode.isBlank() ? search : templateCode;
    }

    // getters and setters
}
```

## 6. Controller 模板

```java
package com.eprint.admin.module.template.controller;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.eprint.admin.module.template.service.TemplateService;
import com.eprint.admin.repository.model.result.TemplateResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public String query(TemplateQueryRequest request, Model model) {
        model.addAttribute("request", request);
        return "template/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> query(TemplateQueryRequest request) {
        PageResult<TemplateResult> pageResult = templateService.query(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", pageResult.getTotal());
        body.put("rows", pageResult.getRecords());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("request", new TemplateCreateRequest());
        return "template/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("request") TemplateCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "template/create";
        }
        templateService.create(request);
        redirectAttributes.addFlashAttribute("message", "Template created");
        return "redirect:/admin/templates";
    }

    @GetMapping("/modify")
    public String modify(TemplateModifyRequest request, Model model) {
        model.addAttribute("request", templateService.getModifyRequest(request));
        return "template/modify";
    }

    @PostMapping("/modify")
    public String modify(@Valid @ModelAttribute("request") TemplateModifyRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "template/modify";
        }
        templateService.modify(request);
        redirectAttributes.addFlashAttribute("message", "Template saved");
        return "redirect:/admin/templates";
    }
}
```

规则：

- 页面方法返回模板名。
- 数据接口返回 `ResponseEntity<Map<String, Object>>`。
- 表单模型属性统一命名为 `request`。
- 不在 Controller 中创建 Entity。
- 不在 Controller 中拼接 SQL 排序字段。

## 7. Service 模板

```java
package com.eprint.admin.module.template.service;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.eprint.admin.repository.dao.TemplateDao;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import com.eprint.admin.repository.model.result.TemplateResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TemplateService {

    private final TemplateDao templateDao;

    public TemplateService(TemplateDao templateDao) {
        this.templateDao = templateDao;
    }

    public PageResult<TemplateResult> query(TemplateQueryRequest request) {
        TemplateQueryParam param = new TemplateQueryParam();
        param.setTemplateTypeId(request.getTemplateTypeId());
        param.setTemplateCode(request.queryTemplateCode());
        param.setStatus(request.getStatus());
        param.setOrderBy(resolveOrderBy(request.getSort()));
        param.setPage(request.page());
        param.setPageSize(request.limit());
        return new PageResult<>(templateDao.query(param), templateDao.count(param));
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateCreateRequest request) {
        Template template = toEntity(request);
        templateDao.create(template);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(TemplateModifyRequest request) {
        Template template = toEntity(request);
        template.setId(request.getId());
        templateDao.modify(template);
    }

    private String resolveOrderBy(String sort) {
        Map<String, String> allowed = Map.of(
                "templateType", "TT.NAME",
                "templateCode", "T.CODE",
                "status", "T.STATUS"
        );
        // parse sort and only emit allowed columns
        return "T.ID DESC";
    }

    private Template toEntity(TemplateCreateRequest request) {
        Template template = new Template();
        template.setTemplateTypeId(request.getTemplateTypeId());
        template.setTemplateCode(request.getTemplateCode());
        template.setBucketName(request.getBucketName());
        template.setObjectName(request.getObjectName());
        template.setStatus(request.getStatus());
        return template;
    }
}
```

规则：

- Service 接收 Web Request。
- Service 转换 Request 到 Entity/Param。
- Service 负责唯一性校验、关联校验、删除前检查。
- Service 负责排序白名单。
- 写操作加事务。
- 批量操作不绕过业务约束。

## 8. Dao 模板

```java
package com.eprint.admin.repository.dao;

import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import com.eprint.admin.repository.model.result.TemplateResult;
import com.niko.boot.dao.BaseDao;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "TemplateDao")
public class TemplateDao extends BaseDao {

    @Resource(name = "sqlSessionTemplateMybatis")
    private SqlSession sqlSession;

    @Override
    protected SqlSession getSqlSession() {
        return sqlSession;
    }

    @Override
    protected String getSqlNamespace() {
        return getClass().getName();
    }

    public List<TemplateResult> queryByType(String templateTypeId) {
        return query("queryByType", templateTypeId);
    }
}
```

规则：

- Dao 必须继承 `BaseDao`。
- 必须注入 `@Resource(name = "sqlSessionTemplateMybatis") SqlSession`。
- 必须覆写 `getSqlSession()`。
- 必须覆写 `getSqlNamespace()`，返回 `getClass().getName()`。
- XML namespace 等于 Dao 完整类名。
- 基础 CRUD 和分页查询必须使用 BaseDao 方法：`create()`、`modify()`、`remove()`、`get()`、`query()`、`count()`。
- Dao 不直接调用 `sqlSession.selectList()`、`sqlSession.selectOne()`、`sqlSession.insert()`、`sqlSession.update()`、`sqlSession.delete()`。
- Dao 只为特殊查询增加类型化方法，例如 `queryByType()`、`countTemplates()`。
- 自定义 Dao 方法的 statement id 必须与方法名一致。
- 不在 Dao 做业务判断。

## 9. Entity、Param 和 Result 模板

Entity：

```java
package com.eprint.admin.repository.model.entity;

import java.io.Serializable;

public class Template implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateTypeId;
    private String templateCode;
    private String bucketName;
    private String objectName;
    private Integer status;

    // getters and setters
}
```

Result：

```java
package com.eprint.admin.repository.model.result;

import com.eprint.admin.repository.model.entity.Template;

public class TemplateResult extends Template {
    private static final long serialVersionUID = 1L;

    private String templateType;
    private String templateTypeName;

    // getters and setters
}
```

QueryParam：

```java
package com.eprint.admin.repository.model.param;

public class TemplateQueryParam {

    private String templateTypeId;
    private String templateCode;
    private Integer status;
    private String orderBy;
    private Integer page;
    private Integer pageSize;

    public int getRowStart() {
        return (page - 1) * pageSize;
    }

    public int getRowEnd() {
        return page * pageSize;
    }

    // getters and setters
}
```

规则：

- Entity 字段使用 Java 小驼峰，只包含表内字段。
- Result 字段使用 Java 小驼峰，用于承载查询返回值和关联展示字段。
- 单表查询 Result 继承 Entity；多表或复杂查询 Result 可独立实现 `Serializable`。
- QueryParam 只服务 MyBatis 查询。
- QueryParam 不使用 Web 层命名，例如 `offset`、`limit`；统一转换为 `page`、`pageSize`、`rowStart`、`rowEnd`。

## 10. MyBatis XML 模板

位置：

```text
e-print-admin/src/main/java/com/eprint/admin/repository/mapper/{Module}.xml
```

模板：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.eprint.admin.repository.dao.TemplateDao">

    <resultMap type="com.eprint.admin.repository.model.result.TemplateResult" id="templateResult">
        <result property="id" column="ID"/>
        <result property="templateTypeId" column="TEMPLATE_TYPE_ID"/>
        <result property="templateType" column="TEMPLATE_TYPE"/>
        <result property="templateTypeName" column="TEMPLATE_TYPE_NAME"/>
        <result property="templateCode" column="CODE"/>
        <result property="bucketName" column="BUCKET_NAME"/>
        <result property="objectName" column="OBJECT_NAME"/>
        <result property="status" column="STATUS"/>
    </resultMap>

    <select id="query"
            parameterType="com.eprint.admin.repository.model.param.TemplateQueryParam"
            resultMap="templateResult">
        SELECT
            ID,
            TEMPLATE_TYPE_ID,
            TEMPLATE_TYPE,
            TEMPLATE_TYPE_NAME,
            CODE,
            BUCKET_NAME,
            OBJECT_NAME,
            STATUS
        FROM (
            SELECT
                T.ID,
                T.TEMPLATE_TYPE_ID,
                TT.CODE AS TEMPLATE_TYPE,
                TT.NAME AS TEMPLATE_TYPE_NAME,
                T.CODE,
                T.BUCKET_NAME,
                T.OBJECT_NAME,
                T.STATUS,
                ROW_NUMBER() OVER (
                    ORDER BY
                    <choose>
                        <when test="orderBy != null and orderBy != ''">${orderBy}</when>
                        <otherwise>T.ID DESC</otherwise>
                    </choose>
                ) RN
            FROM E_PRINT_TEMPLATE T
            LEFT JOIN E_PRINT_TEMPLATE_TYPE TT ON TT.ID = T.TEMPLATE_TYPE_ID
            WHERE 1 = 1
            <if test="templateTypeId != null and templateTypeId != ''">
                AND T.TEMPLATE_TYPE_ID = #{templateTypeId}
            </if>
            <if test="templateCode != null and templateCode != ''">
                AND LOWER(T.CODE) LIKE '%' || LOWER(#{templateCode}) || '%'
            </if>
            <if test="status != null">
                AND T.STATUS = #{status}
            </if>
        )
        WHERE RN &gt; #{rowStart}
          AND RN &lt;= #{rowEnd}
        ORDER BY RN
    </select>

    <select id="count"
            parameterType="com.eprint.admin.repository.model.param.TemplateQueryParam"
            resultType="int">
        SELECT COUNT(1)
        FROM E_PRINT_TEMPLATE T
        WHERE 1 = 1
        <if test="templateTypeId != null and templateTypeId != ''">
            AND T.TEMPLATE_TYPE_ID = #{templateTypeId}
        </if>
        <if test="templateCode != null and templateCode != ''">
            AND LOWER(T.CODE) LIKE '%' || LOWER(#{templateCode}) || '%'
        </if>
        <if test="status != null">
            AND T.STATUS = #{status}
        </if>
    </select>

    <insert id="create" parameterType="com.eprint.admin.repository.model.entity.Template">
        INSERT INTO E_PRINT_TEMPLATE (
            ID,
            TEMPLATE_TYPE_ID,
            CODE,
            BUCKET_NAME,
            OBJECT_NAME,
            STATUS
        ) VALUES (
            SEQ_E_PRINT_TEMPLATE.NEXTVAL,
            #{templateTypeId},
            #{templateCode},
            #{bucketName},
            #{objectName},
            #{status}
        )
    </insert>

    <update id="modify" parameterType="com.eprint.admin.repository.model.entity.Template">
        UPDATE E_PRINT_TEMPLATE
        SET
            TEMPLATE_TYPE_ID = #{templateTypeId},
            CODE = #{templateCode},
            BUCKET_NAME = #{bucketName},
            OBJECT_NAME = #{objectName},
            STATUS = #{status}
        WHERE ID = #{id}
    </update>

    <delete id="remove">
        DELETE FROM E_PRINT_TEMPLATE
        WHERE ID IN
        <foreach collection="array" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </delete>

</mapper>
```

规则：

- 普通参数只使用 `#{}`。
- `${orderBy}` 只允许接收 Service 白名单生成值。
- `query` 和 `count` 查询条件必须一致。
- 所有关联字段必须显式别名。
- `resultMap` 必须显式映射。
- `INSERT` 使用 `SEQ_{TABLE}.NEXTVAL`。

## 11. 数据库模板

表字段：

- 主键：`ID`
- 编码：`CODE`
- 名称：`NAME`
- 状态：`STATUS`
- 排序：`SORT_NO`
- 外键：`{REFERENCED_TABLE_NAME}_ID` 或业务上更清晰的 `{DOMAIN}_ID`

约束和索引命名：

- 主键：`PK_{TABLE}_00`
- 唯一索引：`UK_{TABLE}_00`
- 普通索引：`IDX_{TABLE}_00`
- 外键：`FK_{TABLE}_00`
- 序列：`SEQ_{TABLE}`

规则：

- 后缀使用双数字，从 `00` 开始。
- `SEQ_*` 不加数字后缀。
- Oracle 标识符控制在 30 字符以内。
- 表名、字段名、索引名、约束名统一大写。

## 12. Thymeleaf 页面模板

每个页面独立文件：

```text
templates/{module}/query.html
templates/{module}/create.html
templates/{module}/modify.html
templates/{module}/preview.html
```

列表页标准：

- 页面标题、导航名称、列表标题一致。
- 查询区在表格上方。
- Bootstrap Table 开启分页、固定表头/列、多列排序。
- 批量按钮和新增按钮放在表格抬头按钮区。
- 新增按钮在最右侧。
- 表格数据接口返回 `{ total, rows }`。
- 表格列默认不换行。
- 长文本使用省略、tooltip 或横向滚动。
- 选中行和固定列选中样式保持一致。

表单页标准：

- `create.html` 只服务新增。
- `modify.html` 只服务编辑。
- 模型属性统一 `request`。
- 表单 action 分别指向 `/create`、`/modify`。
- 单表单页面居中，宽度受控。
- 按钮区包含保存、返回。

## 13. 生成检查清单

生成或重构模块时按顺序完成：

1. Controller。
2. Request。
3. Service。
4. Entity。
5. QueryParam。
6. Dao。
7. Mapper XML。
8. Thymeleaf 页面。
9. 静态 JS/CSS。
10. Oracle DDL/DML。
11. README 或模块说明。

验证：

- 改动 JS：运行 `node --check`。
- 改动 Java：运行 `mvn -DskipTests compile`，必须使用 JDK 21。
- 改动 skill：运行 skill `quick_validate.py`。
