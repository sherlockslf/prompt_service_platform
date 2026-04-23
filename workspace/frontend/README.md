# PSU全生命周期管理平台 - 前端

## 项目概述
PSU（Prompt Service Unit）全生命周期管理平台前端，基于Vue 3 + Element Plus + Vite构建。

## 技术栈
- Vue 3 (Composition API)
- Element Plus (UI组件库)
- Vite (构建工具)
- Pinia (状态管理)
- Vue Router (路由管理)
- Axios (HTTP客户端)

## 项目结构
```
src/
├── assets/                 # 静态资源
├── components/            # 公共组件
│   └── Layout.vue        # 公共布局组件
├── views/                 # 页面视图
│   ├── auth/             # 认证相关页面
│   │   └── Login.vue     # 登录页面
│   ├── admin/            # 管理员页面
│   │   └── Dashboard.vue # 管理员仪表板
│   ├── developer/        # 研发页面
│   │   └── Dashboard.vue # 研发仪表板
│   └── business/         # 业务页面
│       └── Dashboard.vue # 业务仪表板
├── stores/                # Pinia状态管理
│   └── user.js           # 用户状态
├── services/              # API服务
│   └── api.js            # API接口定义
├── router/                # 路由配置
├── App.vue               # 根组件
└── main.js               # 入口文件
```

## 功能模块

### 1. 认证模块
- 用户登录
- 权限验证
- Token管理

### 2. 管理员模块
- 用户管理
- 系统配置
- 审计日志

### 3. 研发模块
- PSU管理
- Schema编辑器
- Prompt管理
- 版本审核
- 代码生成

### 4. 业务模块
- PSU列表
- Prompt编排调试
- 在线调试
- 版本提交

## 启动项目

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 预览生产版本
```bash
npm run preview
```

## 环境配置
开发环境下，API请求会被代理到后端服务（默认端口8080）。

## 代码规范
- 使用ESLint + Prettier进行代码格式化
- 组件命名采用PascalCase
- 文件命名采用kebab-case
- 代码注释使用中文

## API接口
所有API接口都通过axios进行封装，包含请求/响应拦截器处理认证和错误。

## 权限控制
- 基于角色的路由守卫
- 页面级权限控制
- 操作级权限控制