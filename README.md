# 课程作业查重与评阅系统

> Homework Plagiarism Detection & Review System

基于 Spring Boot 3 + Vue 3 的课程作业管理平台，支持作业提交、双算法文本查重、教师评阅、实时通知广播和数据分析等功能。

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
| 作业提交 | 学生提交作业文件或在线文本，支持多版本 |
| 查重检测 | SimHash + Jaccard 双算法融合，生成两两对比报告 |
| 教师评阅 | 打分、批注、批量评阅、版本演化分析 |
| 通知广播 | 管理员/教师可向全员或指定角色推送公告，WebSocket 实时接收 |
| 用户管理 | 管理员查看/启停用户账号 |
| 数据分析 | 提交趋势、成绩分布、查重统计 |
| 审计日志 | 操作日志记录与查询 |
| 系统监控 | 性能指标、系统健康检查 |

## 查重算法

系统采用 **SimHash + Jaccard 双算法加权融合**：

| 算法 | 权重 | 原理 |
|------|------|------|
| SimHash | 70% | 文本指纹 + 汉明距离，对语序改动鲁棒 |
| Jaccard | 30% | 词集合交并比，精准捕捉关键词重复 |

最终相似度 = `SimHash × 0.70 + Jaccard × 0.30`，权重可在创建检测任务时自定义。

## 项目结构

```
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/       # Java 源码
│   │   └── com/example/homework/
│   │       ├── common/      # 通用工具类
│   │       ├── config/      # 配置类
│   │       ├── controller/  # 控制器
│   │       ├── domain/      # 实体、DTO、VO
│   │       ├── mapper/      # MyBatis 映射
│   │       ├── service/     # 业务逻辑层
│   │       └── util/        # 工具类（SimHash、Jaccard）
│   ├── src/main/resources/
│   │   ├── sql/init.sql     # 数据库初始化
│   │   └── application.yml  # 应用配置
│   └── src/test/            # 单元测试 & 集成测试
├── frontend/                # Vue 3 前端
│   ├── src/
│   │   ├── api/             # API 接口模块
│   │   ├── components/      # 公共组件（AppShell）
│   │   ├── router/          # 路由配置与权限守卫
│   │   ├── views/           # 页面视图
│   │   └── utils/           # 工具函数
│   └── vite.config.ts
├── monitoring/              # Prometheus + Grafana 配置
├── ops/                     # 运维部署脚本
├── scripts/                 # 自动化脚本
├── docs/                    # 项目文档
├── dev.sh                   # 一键启动脚本
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

### 2. 一键启动（推荐）

```bash
bash dev.sh
```

脚本会自动编译后端 JAR 并启动前后端：

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 | http://localhost:8081 |

也可单独启动：

```bash
bash dev.sh backend   # 仅后端
bash dev.sh frontend  # 仅前端
```

### 3. 手动启动

```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

## 角色说明

| 角色 | 权限 |
|------|------|
| `ADMIN` | 系统管理、用户管理、全局配置、通知广播 |
| `TEACHER` | 课程管理、发布作业、评阅打分、查重管理、通知广播 |
| `STUDENT` | 查看课程、提交作业、查看成绩与通知 |

## License

MIT
