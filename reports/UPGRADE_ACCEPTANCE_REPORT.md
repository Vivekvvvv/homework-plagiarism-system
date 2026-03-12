# 升级完成验收报告

验收日期：2026-03-14  
项目：作业查重与评阅辅助系统

## 1. 结论
本次升级验收通过。核心链路冒烟测试与部署后回归检查均通过，图表与截图材料齐备，性能基线已归档。

## 2. 验收范围
- 登录、课程、作业、提交、查重、评阅、导出主链路
- 冒烟脚本执行与导出文件完整性校验
- 部署后回归流程
- 图表与截图素材归档
- 性能基线归档

## 3. 验收环境
- 后端：Spring Boot（本机运行）
- 前端：未启动（本次验收为接口链路验证）
- 数据库：MySQL 8.x（本机 127.0.0.1:3306）
- 运行端口：后端 8081

## 4. 执行记录
- 冒烟测试结果：`C:\Users\31628\Desktop\work\artifacts\smoke\smoke_result_20260314150105.json`
- 部署后回归：通过（复用最新冒烟结果文件）

## 5. 关键证据

### 5.1 冒烟与回归结果
- 冒烟结果文件：`C:\Users\31628\Desktop\work\artifacts\smoke\smoke_result_20260314150105.json`
- 导出文件：
  - `C:\Users\31628\Desktop\work\artifacts\smoke\plagiarism_pairs_8.csv`
  - `C:\Users\31628\Desktop\work\artifacts\smoke\assignment_9_report.csv`
  - `C:\Users\31628\Desktop\work\artifacts\smoke\assignment_9_reviews.csv`

### 5.2 性能基线
- `C:\Users\31628\Desktop\work\artifacts\perf\perf_result_20260312160530.json`

### 5.3 图表与截图
- 图表目录：`C:\Users\31628\Desktop\work\artifacts\charts\p2\`
- 截图目录：`C:\Users\31628\Desktop\work\artifacts\screenshots\p2\`

## 6. 结果摘要
- 冒烟结果（2026-03-14）：
  - baseUrl：`http://localhost:8081/api/v1`
  - 课程/作业创建：成功
  - 查重任务完成：成功（taskId=8）
  - 评阅与导出：成功
  - reviewedRate：0.3333（已归一化到 0~1）
  - reviewSuggestion：中文显示正常
- 回归检查：通过

## 7. 未覆盖与风险提示
- 本次验收未运行 `mvn test`、`npm test`、`vue-tsc --noEmit`，如需完整测试覆盖需另行补跑。
- 性能基线仅覆盖 `/actuator/health`，生产前建议按关键接口补充基线。

## 8. 结论说明
本次升级验收满足当前验收清单要求，材料与证据齐全，可进入交付阶段。
