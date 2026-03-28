<template>
  <AppShell title="数据看板">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="多维统计" name="overview">
        <el-card>
          <template #header>
            <div class="panel-header">
              <span>课程 / 作业 / 提交统计</span>
              <el-button @click="loadOverview">刷新统计</el-button>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col :span="6">
              <el-statistic title="课程数" :value="overview.courseStats.length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="作业数" :value="overview.assignmentStats.length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="学生数" :value="overview.studentStats.length" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="平均得分" :value="overviewAverageScore" />
            </el-col>
          </el-row>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>课程维度</span>
          </template>
          <el-table :data="overview.courseStats" border size="small">
            <el-table-column prop="courseId" label="课程ID" width="90" />
            <el-table-column prop="courseName" label="课程" min-width="160" />
            <el-table-column prop="courseCode" label="代码" width="120" />
            <el-table-column prop="semester" label="学期" width="120" />
            <el-table-column prop="assignmentCount" label="作业数" width="90" />
            <el-table-column prop="submissionCount" label="提交数" width="90" />
            <el-table-column prop="reviewedCount" label="已评阅" width="90" />
            <el-table-column prop="averageScore" label="平均分" width="90" />
            <el-table-column prop="highRiskAssignments" label="高风险作业" width="110" />
          </el-table>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>作业维度</span>
          </template>
          <el-table :data="overview.assignmentStats" border size="small">
            <el-table-column prop="assignmentId" label="作业ID" width="90" />
            <el-table-column prop="title" label="作业" min-width="160" />
            <el-table-column prop="courseName" label="课程" min-width="140" />
            <el-table-column prop="deadline" label="截止时间" width="170" />
            <el-table-column prop="submissionCount" label="提交数" width="90" />
            <el-table-column prop="reviewedCount" label="已评阅" width="90" />
            <el-table-column prop="averageScore" label="平均分" width="90" />
            <el-table-column prop="highRiskPairs" label="高风险对" width="100" />
            <el-table-column prop="latestTaskStatus" label="查重状态" width="100" />
          </el-table>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>学生维度</span>
          </template>
          <el-table :data="overview.studentStats" border size="small">
            <el-table-column prop="studentId" label="学生ID" width="90" />
            <el-table-column prop="submissionCount" label="提交数" width="90" />
            <el-table-column prop="reviewedCount" label="已评阅" width="90" />
            <el-table-column prop="averageScore" label="平均分" width="90" />
            <el-table-column prop="latestSubmitTime" label="最近提交" min-width="170" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="系统指标" name="metrics">
        <el-card>
          <template #header>
            <div class="panel-header">
              <span>系统运行指标</span>
              <el-button @click="loadMetrics">刷新指标</el-button>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col :span="6"><el-statistic title="用户" :value="metrics.userCount" /></el-col>
            <el-col :span="6"><el-statistic title="教师" :value="metrics.teacherCount" /></el-col>
            <el-col :span="6"><el-statistic title="学生" :value="metrics.studentCount" /></el-col>
            <el-col :span="6"><el-statistic title="课程" :value="metrics.courseCount" /></el-col>
          </el-row>
          <el-row :gutter="12" style="margin-top: 12px">
            <el-col :span="6"><el-statistic title="作业" :value="metrics.assignmentCount" /></el-col>
            <el-col :span="6"><el-statistic title="提交" :value="metrics.submissionCount" /></el-col>
            <el-col :span="6"><el-statistic title="评阅" :value="metrics.reviewCount" /></el-col>
            <el-col :span="6"><el-statistic title="查重任务" :value="metrics.plagiarismTaskCount" /></el-col>
          </el-row>
          <el-row :gutter="12" style="margin-top: 12px">
            <el-col :span="6"><el-statistic title="高风险对" :value="metrics.plagiarismHighRiskPairs" /></el-col>
            <el-col :span="6">
              <el-statistic title="最近提交" :value="0" :formatter="formatLatestSubmission" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="最近评阅" :value="0" :formatter="formatLatestReview" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="最近查重" :value="0" :formatter="formatLatestPlagiarism" />
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="性能基线" name="perf">
        <el-card>
          <template #header>
            <div class="panel-header">
              <span>性能基线结果</span>
              <div class="panel-actions">
                <el-button @click="loadPerfBaselines">刷新</el-button>
                <el-button type="primary" @click="openPerfDialog">导入基线</el-button>
              </div>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col :span="6"><el-statistic title="最近 P95(ms)" :value="latestPerf.p95Ms || 0" /></el-col>
            <el-col :span="6"><el-statistic title="最近 Avg(ms)" :value="latestPerf.avgMs || 0" /></el-col>
            <el-col :span="6"><el-statistic title="错误率(%)" :value="latestErrorRate" /></el-col>
            <el-col :span="6">
              <el-statistic title="生成时间" :value="0" :formatter="formatLatestPerfTime" />
            </el-col>
          </el-row>

          <el-table :data="perfBaselines" border size="small" style="margin-top: 12px">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="baseUrl" label="BaseUrl" min-width="160" />
            <el-table-column prop="path" label="Path" width="140" />
            <el-table-column prop="requests" label="请求数" width="90" />
            <el-table-column prop="success" label="成功" width="80" />
            <el-table-column prop="failed" label="失败" width="80" />
            <el-table-column label="错误率(%)" width="90">
              <template #default="{ row }">{{ ((Number(row.errorRate || 0) * 100)).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="avgMs" label="Avg(ms)" width="90" />
            <el-table-column prop="p95Ms" label="P95(ms)" width="90" />
            <el-table-column prop="maxMs" label="Max(ms)" width="90" />
            <el-table-column prop="generatedAt" label="生成时间" min-width="170" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="perfDialogVisible" title="导入性能基线" width="620px">
      <el-form label-width="90px">
        <el-form-item label="JSON结果">
          <el-input v-model="perfJson" type="textarea" :rows="10" placeholder="粘贴 perf-check 输出 JSON" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="perfDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPerf" @click="savePerfBaseline">解析并保存</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AppShell from "../components/AppShell.vue";
import {
  analyticsOverviewApi,
  createPerfBaselineApi,
  listPerfBaselinesApi,
  systemMetricsApi,
} from "../api/modules";
import { notifyApiError } from "../utils/notify";

type Overview = {
  courseStats: any[];
  assignmentStats: any[];
  studentStats: any[];
};

const activeTab = ref("overview");
const overview = reactive<Overview>({
  courseStats: [],
  assignmentStats: [],
  studentStats: [],
});

const metrics = reactive<any>({
  userCount: 0,
  teacherCount: 0,
  studentCount: 0,
  courseCount: 0,
  assignmentCount: 0,
  submissionCount: 0,
  reviewCount: 0,
  plagiarismTaskCount: 0,
  plagiarismHighRiskPairs: 0,
  latestSubmissionTime: "",
  latestReviewTime: "",
  latestPlagiarismTaskTime: "",
});

const perfBaselines = ref<any[]>([]);
const perfDialogVisible = ref(false);
const perfJson = ref("");
const savingPerf = ref(false);

const overviewAverageScore = computed(() => {
  if (overview.assignmentStats.length === 0) return 0;
  const valid = overview.assignmentStats.filter((item) => item.averageScore > 0);
  if (valid.length === 0) return 0;
  const avg = valid.reduce((sum, item) => sum + Number(item.averageScore || 0), 0) / valid.length;
  return Number(avg.toFixed(2));
});

const latestPerf = computed(() => perfBaselines.value[0] || {});
const latestErrorRate = computed(() => Number(((Number(latestPerf.value.errorRate || 0)) * 100).toFixed(2)));

const formatLatestSubmission = () => metrics.latestSubmissionTime || "-";
const formatLatestReview = () => metrics.latestReviewTime || "-";
const formatLatestPlagiarism = () => metrics.latestPlagiarismTaskTime || "-";
const formatLatestPerfTime = () => latestPerf.value.generatedAt || "-";

const loadOverview = async () => {
  try {
    const res = await analyticsOverviewApi();
    overview.courseStats = res.data.courseStats || [];
    overview.assignmentStats = res.data.assignmentStats || [];
    overview.studentStats = res.data.studentStats || [];
  } catch (error) {
    notifyApiError(error, "加载统计失败");
  }
};

const loadMetrics = async () => {
  try {
    const res = await systemMetricsApi();
    Object.assign(metrics, res.data || {});
  } catch (error) {
    notifyApiError(error, "加载系统指标失败");
  }
};

const loadPerfBaselines = async () => {
  try {
    const res = await listPerfBaselinesApi(50);
    perfBaselines.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    notifyApiError(error, "加载性能基线失败");
  }
};

const openPerfDialog = () => {
  perfJson.value = "";
  perfDialogVisible.value = true;
};

const savePerfBaseline = async () => {
  if (!perfJson.value.trim()) {
    ElMessage.warning("请先粘贴 JSON 结果");
    return;
  }
  savingPerf.value = true;
  try {
    const parsed = JSON.parse(perfJson.value);
    await createPerfBaselineApi({
      baseUrl: String(parsed.baseUrl || ""),
      path: String(parsed.path || ""),
      requests: Number(parsed.requests || 0),
      success: Number(parsed.success || 0),
      failed: Number(parsed.failed || 0),
      errorRate: Number(parsed.errorRate || 0),
      minMs: Number(parsed.minMs || 0),
      avgMs: Number(parsed.avgMs || 0),
      p95Ms: Number(parsed.p95Ms || 0),
      maxMs: Number(parsed.maxMs || 0),
      generatedAt: parsed.generatedAt || undefined,
    });
    ElMessage.success("性能基线已保存");
    perfDialogVisible.value = false;
    await loadPerfBaselines();
  } catch (error) {
    notifyApiError(error, "保存性能基线失败");
  } finally {
    savingPerf.value = false;
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadMetrics(), loadPerfBaselines()]);
});
</script>

<style scoped>
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-card) {
  border-radius: 14px;
  border-color: #d1fae5;
  box-shadow: 0 2px 12px rgba(22, 163, 127, 0.07);
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, #f0fdf8, #e6f7f3);
  border-bottom-color: #d1fae5;
  font-weight: 700;
  color: #1a2e26;
}

:deep(.el-table th) {
  background: #f0fdf8 !important;
  color: #1a2e26 !important;
  font-weight: 600;
}

:deep(.el-tabs__item.is-active) {
  color: #16a37f;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: #16a37f;
}

:deep(.el-statistic__number) {
  color: #16a37f;
  font-weight: 700;
}
</style>
