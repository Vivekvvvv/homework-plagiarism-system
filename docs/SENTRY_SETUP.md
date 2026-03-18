# Sentry 监控接入指南

## 1. 创建 Sentry 项目

1. 注册/登录 [https://sentry.io](https://sentry.io)（或自建 Sentry 实例）
2. 创建两个项目：
   - **Spring Boot** 平台 → 用于后端
   - **Vue** 平台 → 用于前端
3. 记录各自生成的 DSN（格式：`https://<public-key>@<host>.ingest.sentry.io/<project-id>`）

## 2. 后端配置

依赖已安装：`sentry-spring-boot-starter-jakarta:7.8.0`（`pom.xml`）。

### 设置环境变量

```bash
export SENTRY_DSN=https://your-backend-dsn@xxx.ingest.sentry.io/xxx
export SENTRY_ENV=production   # 或 development / staging
```

### application.yml（已配置好，无需修改）

```yaml
sentry:
  dsn: ${SENTRY_DSN:}           # 空值 = 禁用 Sentry
  environment: ${SENTRY_ENV:development}
  traces-sample-rate: 1.0
```

### 过滤规则

`SentryConfig.java` 已配置 `BeforeSendCallback`，自动过滤 `BusinessException`（业务异常不上报）。仅非预期异常（NullPointerException、数据库超时等）会上报。

## 3. 前端配置

依赖已安装：`@sentry/vue:^8.0.0`（`package.json`）。

### 设置环境变量

在 `.env.production` 或部署环境中设置：

```
VITE_SENTRY_DSN=https://your-frontend-dsn@xxx.ingest.sentry.io/xxx
```

### main.ts（已配置好，无需修改）

```typescript
const sentryDsn = import.meta.env.VITE_SENTRY_DSN;
if (sentryDsn) {
  Sentry.init({
    app,
    dsn: sentryDsn,
    integrations: [Sentry.browserTracingIntegration({ router })],
    tracesSampleRate: 1.0,
  });
}
```

当 `VITE_SENTRY_DSN` 为空时，Sentry 不会初始化，零开销。

## 4. 验证

1. **后端：** 启动后端，故意触发一个未捕获异常（如访问不存在的端点），在 Sentry Dashboard 查看是否出现事件
2. **前端：** 启动前端，在浏览器控制台执行 `throw new Error('sentry test')`，检查 Sentry Dashboard
3. **性能监控：** 访问几个 API 端点，在 Sentry Performance 页面查看 Trace 数据

## 5. 生产建议

- 将 `traces-sample-rate` 从 `1.0` 降低到 `0.1`~`0.3`（减少性能开销）
- 配置 Release 版本追踪（`sentry.release` 或 `SENTRY_RELEASE` 环境变量）
- 设置告警规则（如：新错误通知、错误频率突增）
