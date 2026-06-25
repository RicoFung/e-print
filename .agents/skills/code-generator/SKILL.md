---
name: code-generator
description: Generate or refactor e-print-admin module code using this project's Spring MVC, Thymeleaf, MyBatis, Oracle, Bootstrap Table, layered module/repository structure, route/template naming, Request DTO, and admin UI conventions. Use when adding or modifying e-print-admin CRUD pages, Controller/Service/Dao/Mapper/Request classes, Thymeleaf pages, template/type management flows, or code-generator rules for the e-print-admin project.
---

# e-print Code Generator

## Overview

Use this skill when generating or refactoring `e-print-admin` backend page code. It enforces project-specific conventions for layered code structure, Controller routes, method names, request DTOs, Thymeleaf page names, service boundaries, Dao/MyBatis usage, Oracle SQL, Bootstrap Table behavior, and validation.

Currently, this skill only contains `e-print-admin` rules. Reserve this skill package for future `e-print-server` generation rules, but do not apply server-side conventions until a dedicated server reference is added.

## Required Context

Before changing code, read the project convention document:

```text
references/code-template.md
```

This reference is bundled with the skill under `.agents/skills/code-generator/references`.

## Core Rules

- Keep request path, Controller method name, and Thymeleaf template file name aligned:
  - `/` query page -> `query()` -> `query.html`
  - `/query` table data -> `query()` -> JSON `{ total, rows }`
  - `/create` -> `create()` -> `create.html`
  - `/modify` -> `modify()` -> `modify.html`
  - `/preview` -> `preview()` -> `preview.html`
- Model each request independently with `{Module}{Action}Request`.
- Keep admin page modules under `module/{domain}` and persistence under the shared `repository` package.
- Use Spring MVC `@Controller` for pages; do not generate REST-only controllers for admin pages.
- Do not reuse a create request as a modify request, a query request as a remove request, or a web Request as a MyBatis parameter object.
- Do not use `Admin` in Controller or Service class names.
- Keep Controller thin; put validation, transactions, relation checks, and sorting whitelist logic in Service.
- Keep Dao based on `BaseDao`, with explicit `SqlSession` injection and MyBatis XML mapper namespaces.
- Keep external API semantics separate from admin UI semantics. For templates, external `templateType` means `E_PRINT_TEMPLATE_TYPE.CODE`; admin forms use `templateTypeId`.

## Workflow

1. Inspect existing module files before generating code.
2. Identify the module root path and actions.
3. Create or update action-specific Request classes under `model/request`.
4. Align Controller paths, method names, model attributes, redirects, and view names with the clean convention.
5. Align templates and links with the new paths.
6. Update Service signatures and conversions to accept the new request objects.
7. Keep MyBatis SQL parameter objects in `repository/model/param` separate from web requests.
8. Update Dao and Mapper XML together; namespace must match the Dao class name.
9. Update JS endpoint paths and README entries when routes change.
10. Validate with `node --check` for changed JS and `mvn -DskipTests compile` when JDK 21 is available.

## Repository Patterns

Use these directories:

```text
e-print-admin/src/main/java/com/eprint/admin/module/{module}/controller
e-print-admin/src/main/java/com/eprint/admin/module/{module}/service
e-print-admin/src/main/java/com/eprint/admin/module/{module}/model/request
e-print-admin/src/main/java/com/eprint/admin/repository/dao
e-print-admin/src/main/java/com/eprint/admin/repository/mapper
e-print-admin/src/main/java/com/eprint/admin/repository/model/entity
e-print-admin/src/main/java/com/eprint/admin/repository/model/param
e-print-admin/src/main/java/com/eprint/admin/repository/model/result
e-print-admin/src/main/resources/templates/{module}
e-print-admin/src/main/resources/static/js
e-print-admin/src/main/resources/static/css
```

Prefer the current codebase's Bootstrap Table, SweetAlert, AdminLTE, MyBatis DAO, and Oracle pagination patterns over introducing new frameworks.
