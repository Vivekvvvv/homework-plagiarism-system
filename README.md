# 课程作业查重与评阅系统

> Homework Plagiarism Detection & Review System

基于 Spring Boot 3 + Vue 3 的课程作业管理平台，支持作业提交、代码/文本查重、教师评阅、实时通知和数据分析等功能。

## 技术栈

### 后端
- **Java 21** + **Spring Boot 3.4.2**
- **Spring Security** + JWT 认证
- **MyBatis-Plus** 3.5 — ORM 框架
- **MySQL 8.0** — 关系型数据库
- **WebSocket** — 实时通知推送
- **Sentry** — 错误监控与追踪
- **Actuator + Micrometer** — 指标暴露

### 前端
- **Vue 3.5** + **TypeScript 5.9**
- **Element Plus 2.13** — UI 组件库
- **Pinia** — 状态管理
- **Vue Router 4** — 路由管理
- **Axios** — HTTP 客户端
- **Vite 7** — 构建工具

### 运维与监控
- **Docker** + **Docker Compose** — 容器化部署
- **Prometheus** + **Grafana** — 监控与可视化
- **GitHub Actions** — CI/CD 流水线

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户认证 | 注册、登录、JWT 令牌、密码修改 |
| 课程管理 | 课程的增删改查、学生选课管理 |
| 作业管理 | 教师发布作业、设置截止日期 |
| 作业提交 | 学生提交作业文件、支持多种格式 |
| 查重检测 | 文本/代码相似度检测、查重报告 |
| 教师评阅 | 打分、批注、批量评阅 |
| 实时通知 | WebSocket 推送通知 |
| 数据分析 | 提交趋势、成绩分布、查重统计 |
| 审计日志 | 操作日志记录与查询 |
| 系统监控 | 性能指标、系统健康检查 |

## 项目结构

```
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/       # Java 源码
│   │   └── com/example/homework/
│   │       ├── common/      # 通用工具类
│   │       ├── config/      # 配置类
│   │       ├── controller/  # 控制器（13个）
│   │       ├── domain/      # 实体、DTO、VO
│   │       ├── mapper/      # MyBatis 映射
│   │       ├── service/     # 业务逻辑层
│   │       └── util/        # 工具类
│   ├── src/main/resources/
│   │   ├── sql/init.sql     # 数据库初始化（16张表）
│   │   └── application.yml  # 应用配置
│   └── src/test/            # 单元测试 & 集成测试
├── frontend/                # Vue 3 前端
│   ├── src/
│   │   ├── api/             # API 接口模块
│   │   ├── components/      # 公共组件
│   │   ├── router/          # 路由配置
│   │   ├── views/           # 页面视图
│   │   └── utils/           # 工具函数
│   └── vite.config.ts
├── monitoring/              # Prometheus + Grafana 配置
├── ops/                     # 运维部署脚本
├── scripts/                 # 自动化脚本
├── docs/                    # 项目文档
└── .github/                 # CI/CD 工作流
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.9+

### 1. 初始化数据库

```sql
source backend/src/main/resources/sql/init.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 角色说明

| 角色 | 权限 |
|------|------|
| `ADMIN` | 系统管理、用户管理、全局配置 |
| `TEACHER` | 课程管理、发布作业、评阅打分、查重管理 |
| `STUDENT` | 查看课程、提交作业、查看成绩与通知 |

## License

MIT
