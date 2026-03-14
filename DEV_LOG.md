# 开发日志

## 2026-03-14
- 跑了一轮完整的烟测，5 个场景全部通过
- 修复了查重报告导出时 CSV 编码问题（UTF-8 BOM）
- 前端 dist 构建正常，体积 2.3MB

## 2026-03-12
- 性能基准测试完成，两次结果记录在 artifacts/perf/
- 优化了 PlagiarismService 的异步任务调度
- 发现 ReviewController 的分页查询在数据量大时偏慢，先记到 TODO

## 2026-03-10
- 修复了查重任务超时后状态卡在 running 的 bug
- 原因是 AsyncConfig 里的线程池 rejection handler 没有正确回调
- 加了单元测试验证修复

## 2026-03-09
- GitHub Actions CI 配置完成，push 时自动跑测试
- 编写了全套部署脚本（PowerShell + Bash）
- 生产环境 Nginx 配置搞定，支持 HTTPS

## 2026-03-08
- 登录页在手机上样式崩了，flex 布局没加 min-height
- Element Plus 升级到最新版本，无 breaking change
- 前端路由守卫优化，未登录时重定向到 /login

## 2026-03-01
- 项目初始搭建完成
- 后端 Spring Boot + MyBatis Plus 骨架
- 前端 Vue 3 + Element Plus + Vite 骨架
- 数据库 init.sql 初始化脚本编写完毕
