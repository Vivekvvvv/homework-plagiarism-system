# 升级完成验收简版（答辩）

验收日期：2026-03-14  
项目：作业查重与评阅辅助系统

## 结论
升级完成验收通过，关键主链路与回归验证均通过，材料归档齐全。

## 关键证据
- 冒烟/回归结果：`C:\Users\31628\Desktop\work\artifacts\smoke\smoke_result_20260314150105.json`
- 导出文件：
  - `C:\Users\31628\Desktop\work\artifacts\smoke\plagiarism_pairs_8.csv`
  - `C:\Users\31628\Desktop\work\artifacts\smoke\assignment_9_report.csv`
  - `C:\Users\31628\Desktop\work\artifacts\smoke\assignment_9_reviews.csv`
- 性能基线：`C:\Users\31628\Desktop\work\artifacts\perf\perf_result_20260312160530.json`
- 图表：`C:\Users\31628\Desktop\work\artifacts\charts\p2\`
- 截图：`C:\Users\31628\Desktop\work\artifacts\screenshots\p2\`

## 冒烟与回归摘要
- 主链路：登录 / 课程 / 作业 / 提交 / 查重 / 评阅 / 导出 均通过
- 查重任务：完成（taskId=8）
- reviewedRate：0.3333（归一化 0~1）
- 回归检查：通过

## 说明
- 本次验收未运行 `mvn test`、`npm test`、`vue-tsc --noEmit`，如需完整覆盖需另行补跑。
