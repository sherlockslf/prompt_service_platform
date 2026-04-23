# PSU全生命周期管理平台

> Prompt Service Unit（PSU）全生命周期管理平台，围绕 **Schema 锁定 → Prompt 编排/注入 → 测试集在线调试 → 提审 → 开发审核 → 代码生成 → Git 入库** 的轻量闭环，确保产研协作一致性与可追溯性。

***

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心功能模块](#核心功能模块)
- [角色与权限](#角色与权限)
- [快速开始](#快速开始)
- [数据库设计](#数据库设计)
- [API 接口](#api-接口)
- [配置说明](#配置说明)
- [开发指南](#开发指南)
- [设计文档](#设计文档)
- [注意事项](#注意事项)

***

## 项目概述

本平台是一个面向 Prompt 工程的全生命周期管理系统，主要解决以下问题：

- **Schema 管理**：研发定义和锁定 JSON Schema，规范入参/出参结构
- **Prompt 编排**：业务侧通过可视化编辑器完成提示词编写、变量注入、片段组装
- **在线调试**：绑定测试集进行单条/批量回归测试
- **版本审核**：业务提交后锁定编排，研发审核通过后生成代码
- **代码生成**：自动生成参数校验、参数拼装、Prompt 组装等业务代码

### 版本信息

- **当前版本**：V1.2（动态参数编排增补版）
- **PRD 生效日期**：2026-04-15
- **部署形态**：私有化部署、手工/半自动化发布

***

## 技术栈

### 后端

| 技术                | 版本      | 用途       |
| ----------------- | ------- | -------- |
| Java              | 17      | 运行环境     |
| Spring Boot       | 3.2.0   | 应用框架     |
| Spring Security   | 6.2.0   | 认证授权     |
| Spring Data JPA   | 3.2.0   | 数据访问     |
| MySQL             | 8.0     | 关系型数据库   |
| Redis             | -       | 缓存服务     |
| JWT (jjwt)        | 0.11.5  | Token 管理 |
| Lombok            | 1.18.30 | 代码简化     |
| Hibernate         | 6.3.1   | ORM 框架   |
| SpringDoc OpenAPI | 2.2.0   | API 文档   |
| Maven             | 3.8+    | 构建工具     |

### 前端

| 技术           | 版本    | 用途       |
| ------------ | ----- | -------- |
| Vue          | 3.4   | 前端框架     |
| Element Plus | 2.4.0 | UI 组件库   |
| Pinia        | 2.1.7 | 状态管理     |
| Vue Router   | 4.2.5 | 路由管理     |
| Axios        | 1.6.0 | HTTP 客户端 |
| Vite         | 5.0   | 构建工具     |

***

## 项目结构

```
demo2/
├── config/                          # 配置文件
│   └── nacos_config.txt             # Nacos 远程配置（dev 环境）
│
├── design/                          # 设计文档
│   ├── PRD_PSU全生命周期管理平台_V1.2_增补动态参数编排.md
│   ├── backend-design.md            # 后端设计文档
│   ├── database-design.md           # 数据库设计文档
│   ├── frontend-design.md           # 前端设计文档
│   ├── design_backend-design_V1.2_增补动态参数编排.md
│   ├── design_database-design_V1.2_增补动态参数编排.md
│   ├── design_frontend-design_V1.2_增补动态参数编排.md
│   ├── REQ_前端_动态参数编排容器_PromptComposer_需求与任务清单.md
│   ├── REQ_后端_动态参数编排容器_Composition_需求与接口.md
│   ├── REQ_审核流程_按PSU版本锁定的需求与设计.md
│   └── 清单_文档与实现不一致点_待你确认.md
│
└── workspace/                       # 工作区
    ├── backend/                     # 后端工程（Spring Boot）
    │   ├── src/main/java/           # Java 源码
    │   ├── src/main/resources/      # 配置文件
    │   │   ├── application.yml      # 应用配置
    │   │   └── schema.sql           # 数据库初始化脚本
    │   ├── lib/                     # 依赖 JAR 包
    │   ├── pom.xml                  # Maven 配置
    │   └── README.md                # 后端说明文档
    │
    ├── frontend/                    # 前端工程（Vue 3）
    │   ├── src/
    │   │   ├── components/          # 公共组件
    │   │   │   ├── Layout.vue       # 布局组件
    │   │   │   └── composer/        # 编排容器组件
    │   │   │       ├── PromptTextEditor.vue      # 文本编辑器
    │   │   │       ├── SchemaVariablePanel.vue   # Schema 变量面板
    │   │   │       ├── InjectionSummaryPanel.vue # 注入清单面板
    │   │   │       └── TestDatasetRunner.vue     # 测试集运行器
    │   │   ├── views/               # 页面视图
    │   │   │   ├── auth/Login.vue   # 登录页
    │   │   │   ├── admin/           # 管理员页面
    │   │   │   ├── developer/       # 研发页面
    │   │   │   └── business/        # 业务页面
    │   │   ├── stores/user.js       # 用户状态
    │   │   ├── services/api.js      # API 服务
    │   │   ├── router/index.js      # 路由配置
    │   │   ├── App.vue              # 根组件
    │   │   └── main.js              # 入口文件
    │   ├── dist/                    # 构建产物
    │   ├── index.html
    │   ├── package.json
    │   ├── vite.config.js
    │   └── README.md                # 前端说明文档
    │
    ├── tools/                       # 工具脚本
    │   ├── init_database.bat        # 数据库初始化脚本（Windows）
    │   ├── init_database.py         # 数据库初始化脚本（Python）
    │   ├── update_passwords.bat     # 密码更新脚本
    │   ├── update_passwords_to_plain.py
    │   └── update_version_fields.py
    │
    ├── start-all.bat                # 一键启动前后端
    ├── start-backend.bat            # 启动后端服务
    └── start-frontend.bat           # 启动前端服务
```

***

## 核心功能模块

### 1. 认证授权模块

- JWT Token 认证
- 基于角色的权限控制（ADMIN / DEVELOPER / BUSINESS）
- 用户登录/登出
- 密码加密存储（BCrypt）

### 2. PSU 管理模块

- PSU 单元的创建、查询、归档
- 三位版本号管理（主版本.次版本.修订版本）
- 权限控制（仅研发可创建/修改）

### 3. JSON Schema 管理模块

- Schema 的版本管理
- 权限分级（研发可编辑，业务只读）
- Schema 验证与锁定

### 4. Prompt 管理模块

- Prompt 片段的管理（CORE\_RULES / MESSAGE\_TEMPLATE）
- 权限分级（锁定片段仅研发可编辑）
- Prompt 组装功能

### 5. 动态参数编排容器（Prompt Composer）

- **文本编辑器**：支持 `{{path}}` 占位符语法（Mustache 风格）
- **变量注入**：从 Schema 字段树选择变量插入到文本中
- **编排规格（ComposeSpec）**：结构化存储 tokens、injectionPlan、assembledFragments
- **在线调试**：绑定测试集进行单条/批量运行
- **状态机**：DRAFT → SUBMITTED → DEV\_REVIEWING → APPROVED / REJECTED

### 6. 版本审核模块

- 业务提交版本审核
- 研发审核版本
- 提交后锁定编排（业务侧不可修改）
- 退回策略：区分退回开发改 Schema 或退回业务重编排
- Git 提交记录

### 7. 代码生成模块

- 参数校验代码生成
- 参数拼装代码生成
- Prompt 组装代码生成
- 完整业务代码生成

### 8. 测试数据集模块

- 测试数据集管理
- 批量测试运行
- 测试结果记录（成功/失败、耗时、错误信息）

***

## 角色与权限

| 角色  | 用户名         | 密码         | 权限说明                       |
| --- | ----------- | ---------- | -------------------------- |
| 管理员 | admin\_user | Admin\@123 | 用户管理、系统配置、审计日志             |
| 研发  | dev\_user   | Dev\@123   | 创建 PSU、定义 Schema、审核版本、代码生成 |
| 业务  | bus\_user   | Bus\@123   | Prompt 编排、在线调试、提交审核        |

### 权限矩阵

| 功能           | 管理员 | 研发      | 业务       |
| ------------ | --- | ------- | -------- |
| 用户管理         | ✅   | ❌       | ❌        |
| 创建 PSU       | ❌   | ✅       | ❌        |
| 编辑 Schema    | ❌   | ✅       | 只读       |
| 编辑 Prompt 片段 | ❌   | ✅（锁定片段） | ✅（可编辑片段） |
| Prompt 编排    | ❌   | ✅       | ✅        |
| 在线调试         | ❌   | ✅       | ✅        |
| 提交审核         | ❌   | ❌       | ✅        |
| 审核版本         | ❌   | ✅       | ❌        |
| 代码生成         | ❌   | ✅       | ❌        |

***

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Node.js 16+
- MySQL 8.0
- Redis

### 方式一：一键启动

```bash
cd workspace
start-all.bat
```

该脚本会自动：

1. 检查 Maven 和 Node.js 环境
2. 启动后端服务（Spring Boot）
3. 等待 10 秒后启动前端服务（Vite）

### 方式二：分别启动

#### 启动后端

```bash
cd workspace/backend
mvn spring-boot:run
```

或使用脚本：

```bash
cd workspace
start-backend.bat
```

#### 启动前端

```bash
cd workspace/frontend
npm install
npm run dev
```

或使用脚本：

```bash
cd workspace
start-frontend.bat
```

### 初始化数据库

```bash
cd workspace/tools
init_database.bat
```

### 访问地址

- **前端**：<http://localhost:5173（Vite> 默认端口，具体查看启动日志）
- **后端 API**：<http://localhost:8084>
- **API 文档**：<http://localhost:8084/swagger-ui.html>

***

## 数据库设计

### 表结构概览

| 表名                                 | 说明            |
| ---------------------------------- | ------------- |
| ai\_prompt\_users                  | 用户表           |
| ai\_prompt\_psu                    | PSU 单元表       |
| ai\_prompt\_json\_schemas          | JSON Schema 表 |
| ai\_prompt\_prompt\_fragments      | Prompt 片段表    |
| ai\_prompt\_version\_reviews       | 版本审核表         |
| ai\_prompt\_system\_configs        | 系统配置表         |
| ai\_prompt\_audit\_logs            | 审计日志表         |
| ai\_prompt\_test\_datasets         | 测试数据集表        |
| ai\_prompt\_compositions           | 编排草稿表         |
| ai\_prompt\_composition\_revisions | 编排版本快照表       |
| ai\_prompt\_test\_runs             | 测试运行记录表       |
| ai\_prompt\_test\_run\_items       | 测试运行用例明细表     |

### 核心关系

```
PSU (1) ──→ (N) JSON Schema
PSU (1) ──→ (N) Prompt Fragment
PSU (1) ──→ (1) Composition
PSU (1) ──→ (N) Version Review
Composition (1) ──→ (N) Revision
Test Run (1) ──→ (N) Test Run Item
```

> 注：所有表关系通过应用层 JPA 管理，不使用数据库外键约束。

***

## API 接口

### 认证接口

| 方法   | 路径                | 说明       |
| ---- | ----------------- | -------- |
| POST | /api/auth/login   | 用户登录     |
| GET  | /api/auth/profile | 获取当前用户信息 |

### PSU 管理接口

| 方法   | 路径        | 说明        | 权限 |
| ---- | --------- | --------- | -- |
| POST | /api/psus | 创建 PSU    | 研发 |
| GET  | /api/psus | 获取 PSU 列表 | 全部 |

### Schema 管理接口

| 方法  | 路径                            | 说明             | 权限 |
| --- | ----------------------------- | -------------- | -- |
| GET | /api/schemas/{psuId}          | 获取 Schema      | 全部 |
| PUT | /api/schemas/{psuId}          | 更新 Schema      | 研发 |
| GET | /api/schemas/{psuId}/versions | 获取 Schema 版本历史 | 全部 |

### Prompt 管理接口

| 方法   | 路径                        | 说明           | 权限    |
| ---- | ------------------------- | ------------ | ----- |
| GET  | /api/prompts/{psuId}      | 获取 Prompt 片段 | 全部    |
| PUT  | /api/prompts/{fragmentId} | 更新 Prompt 片段 | 研发/业务 |
| POST | /api/prompts/{psuId}/test | 测试 Prompt    | 全部    |

### 编排接口（Composition）

| 方法   | 路径                                       | 说明        | 权限 |
| ---- | ---------------------------------------- | --------- | -- |
| GET  | /api/compositions?psuId={psuId}          | 获取当前编排草稿  | 业务 |
| PUT  | /api/compositions?psuId={psuId}          | 保存编排草稿    | 业务 |
| POST | /api/compositions/validate?psuId={psuId} | 校验编排      | 业务 |
| POST | /api/compositions/render?psuId={psuId}   | 渲染 Prompt | 业务 |
| POST | /api/compositions/submit?psuId={psuId}   | 提交审核      | 业务 |

### 测试运行接口

| 方法   | 路径                                           | 说明     | 权限 |
| ---- | -------------------------------------------- | ------ | -- |
| POST | /api/test-runs?psuId={psuId}\&datasetId={id} | 批量运行测试 | 业务 |
| GET  | /api/test-runs/{runId}                       | 获取运行详情 | 业务 |

### 版本审核接口

| 方法   | 路径                              | 说明      | 权限 |
| ---- | ------------------------------- | ------- | -- |
| POST | /api/versions/{psuId}/submit    | 提交版本审核  | 业务 |
| POST | /api/versions/{reviewId}/review | 审核版本    | 研发 |
| GET  | /api/versions/{psuId}/code      | 获取生成的代码 | 研发 |

***

## 配置说明

### 应用配置

后端配置文件位于 [workspace/backend/src/main/resources/application.yml](file:///d:/metaspace/trae/java_swarm/demo2/workspace/backend/src/main/resources/application.yml)

主要配置项：

| 配置项                    | 说明       | 默认值           |
| ---------------------- | -------- | ------------- |
| spring.datasource.url  | 数据库连接    | 阿里云 RDS       |
| spring.data.redis.host | Redis 地址 | 阿里云 Redis     |
| llm.provider           | 大模型提供商   | qwen          |
| llm.api-key            | API 密钥   | 多个密钥（逗号分隔）    |
| llm.base-url           | API 地址   | 阿里云 DashScope |
| llm.model              | 模型名称     | qwen-max      |
| security.jwt-secret    | JWT 密钥   | -             |
| server.port            | 服务端口     | 8084          |

### Nacos 配置

远程 Nacos 配置位于 [config/nacos\_config.txt](file:///d:/metaspace/trae/java_swarm/demo2/config/nacos_config.txt)，包含 dev 环境的数据库、Redis、LLM 等配置。

> ⚠️ **注意**：Nacos 配置为远程配置，不要轻易在本地创建配置副本。

***

## 开发指南

### 后端开发

#### 项目包结构

```
com.example.psu/
├── PsuPlatformApplication.java    # 主应用类
├── config/                        # 配置类
│   ├── DatabaseConfig.java        # 数据库配置
│   └── SecurityConfig.java        # 安全配置
├── controller/                    # 控制器层
│   ├── AuthController.java        # 认证控制器
│   ├── PsuController.java         # PSU 管理
│   ├── JsonSchemaController.java  # Schema 管理
│   ├── PromptController.java      # Prompt 管理
│   └── VersionReviewController.java # 版本审核
├── service/                       # 服务层
│   ├── UserService.java
│   ├── PsuService.java
│   ├── JsonSchemaService.java
│   ├── PromptService.java
│   ├── VersionReviewService.java
│   └── CodeGeneratorService.java
├── repository/                    # 数据访问层
│   ├── UserRepository.java
│   ├── PsuRepository.java
│   ├── JsonSchemaRepository.java
│   ├── PromptFragmentRepository.java
│   └── VersionReviewRepository.java
├── entity/                        # 实体类
│   ├── User.java
│   ├── PsuUnit.java
│   ├── JsonSchema.java
│   ├── PromptFragment.java
│   └── VersionReview.java
├── dto/                           # 数据传输对象
│   ├── request/
│   │   ├── UpdateSchemaRequest.java
│   │   ├── UpdatePromptRequest.java
│   │   └── ReviewRequest.java
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   └── PsuCreateRequest.java
├── security/                      # 安全相关
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── SecurityConfig.java
└── util/                          # 工具类
    └── UserContext.java
```

#### 构建与打包

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn clean package

# 运行打包后的应用
java -jar target/psu-platform-1.0.0.jar
```

### 前端开发

#### 项目结构

```
src/
├── components/            # 公共组件
│   ├── Layout.vue        # 布局组件
│   └── composer/         # 编排容器组件
├── views/                 # 页面视图
│   ├── auth/             # 认证页面
│   ├── admin/            # 管理员页面
│   ├── developer/        # 研发页面
│   └── business/         # 业务页面
├── stores/                # Pinia 状态管理
├── services/              # API 服务
├── router/                # 路由配置
├── App.vue               # 根组件
└── main.js               # 入口文件
```

#### 常用命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

### 代码规范

- 代码注释使用中文
- 前端组件命名采用 PascalCase
- 前端文件命名采用 kebab-case
- 遵循现有代码风格和架构模式

***

## 设计文档

| 文档                                                                                                       | 说明                  |
| -------------------------------------------------------------------------------------------------------- | ------------------- |
| [PRD V1.2](file:///d:/metaspace/trae/java_swarm/demo2/design/PRD_PSU全生命周期管理平台_V1.2_增补动态参数编排.md)          | 产品需求文档（动态参数编排增补版）   |
| [后端设计](file:///d:/metaspace/trae/java_swarm/demo2/design/backend-design.md)                              | 后端架构设计              |
| [数据库设计](file:///d:/metaspace/trae/java_swarm/demo2/design/database-design.md)                            | 数据库表结构设计            |
| [前端设计](file:///d:/metaspace/trae/java_swarm/demo2/design/frontend-design.md)                             | 前端架构设计              |
| [后端设计 V1.2](file:///d:/metaspace/trae/java_swarm/demo2/design/design_backend-design_V1.2_增补动态参数编排.md)    | 后端设计 V1.2 增补        |
| [数据库设计 V1.2](file:///d:/metaspace/trae/java_swarm/demo2/design/design_database-design_V1.2_增补动态参数编排.md)  | 数据库设计 V1.2 增补       |
| [前端设计 V1.2](file:///d:/metaspace/trae/java_swarm/demo2/design/design_frontend-design_V1.2_增补动态参数编排.md)   | 前端设计 V1.2 增补        |
| [前端需求与任务清单](file:///d:/metaspace/trae/java_swarm/demo2/design/REQ_前端_动态参数编排容器_PromptComposer_需求与任务清单.md) | PromptComposer 前端需求 |
| [后端需求与接口](file:///d:/metaspace/trae/java_swarm/demo2/design/REQ_后端_动态参数编排容器_Composition_需求与接口.md)        | Composition 后端需求    |
| [审核流程设计](file:///d:/metaspace/trae/java_swarm/demo2/design/REQ_审核流程_按PSU版本锁定的需求与设计.md)                   | 按 PSU 版本锁定的审核流程     |
| [不一致点清单](file:///d:/metaspace/trae/java_swarm/demo2/design/清单_文档与实现不一致点_待你确认.md)                         | 文档与实现不一致点记录         |

***

## 注意事项

### 配置文件

- ⚠️ **不要轻易修改项目的 yaml/yml 配置文件**
- ⚠️ **有些配置是直接写入远程 Nacos 的**
- ⚠️ **不要在本地创建配置副本**，会严重阻碍后续的迭代开发

### 数据库

- 数据库初始化脚本 [schema.sql](file:///d:/metaspace/trae/java_swarm/demo2/workspace/backend/src/main/resources/schema.sql) 会在应用启动时自动执行
- 所有表关系通过应用层 JPA 管理，不使用数据库外键
- 表名统一使用 `ai_prompt_*` 前缀

### 占位符协议

- 编排容器使用 `{{path}}` 占位符语法（Mustache 风格）
- path 以"输入根对象 input"为根，例如：`{{userId}}`、`{{order.items[0].name}}`

### 版本策略

- 三位版本号：主版本.次版本.修订版本
- 研发定 JSON Schema 时，次版本号递增
- 业务定提示词和参数编排时，修订版本号递增
- 审核发布时，主版本号递增

### 审核锁定规则

- 业务提交后（SUBMITTED），编排内容锁定
- 若需修改，只能创建新 DRAFT（新 revision）并重新提审
- 退回时区分：退回开发改 Schema 或退回业务重编排

### 默认账号

如果默认账号登录失败，可运行工具脚本修复：

```bash
cd workspace/tools
update_passwords.bat
```

***

## 相关资源

- [后端 README](file:///d:/metaspace/trae/java_swarm/demo2/workspace/backend/README.md)
- [前端 README](file:///d:/metaspace/trae/java_swarm/demo2/workspace/frontend/README.md)

