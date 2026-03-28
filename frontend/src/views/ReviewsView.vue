<template>
  <AppShell title="评阅中心">
    <el-row :gutter="16">
      <el-col :span="24">
        <el-card class="filter-card">
          <div class="filter-bar">
            <div class="filter-left">
              <span class="filter-label">作业ID</span>
              <el-input-number v-model="assignmentId" :min="1" controls-position="right" />
              <el-button type="primary" @click="reloadAll">查询</el-button>
              <el-button type="primary" plain @click="openRubricDialog">Rubric配置</el-button>
              <el-button :disabled="displayRows.length === 0" @click="exportReviewCsv">导出评阅CSV</el-button>
              <el-button type="warning" :disabled="selectedRows.length === 0" @click="openBatchDialog">
                批量打分 ({{ selectedRows.length }})
              </el-button>
              <el-tag v-if="routeAssignmentId" type="primary">看板定位</el-tag>
              <el-tag v-if="pendingOnlyMode" type="warning">待评阅模式</el-tag>
              <el-button v-if="pendingOnlyMode" link type="primary" @click="switchToAllRows">查看全部</el-button>
              <el-button v-if="routeFromDashboard" link type="primary" @click="backToDashboard">返回工作台</el-button>
            </div>
          </div>
          <el-divider class="filter-divider" />
          <div class="summary-grid">
            <el-statistic title="总提交" :value="summary.totalSubmissions" />
            <el-statistic title="已评阅" :value="summary.reviewedSubmissions" />
            <el-statistic title="评阅率" :value="summaryReviewedRate" :formatter="formatPercent" />
            <el-statistic title="均分" :value="summaryAverageScore" />
            <el-statistic title="及格率" :value="summaryPassRate" :formatter="formatPercent" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card class="table-card">
          <template #header>
            <span>提交评阅列表</span>
          </template>
          <el-table v-if="displayRows.length > 0" :data="displayRows" border stripe size="small" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="submissionId" label="提交ID" width="88" />
            <el-table-column prop="studentId" label="学生ID" width="90" />
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column prop="sourceType" label="来源" width="90">
              <template #default="{ row }">
                {{ row.sourceType === 1 ? "文件" : "文本" }}
              </template>
            </el-table-column>
            <el-table-column prop="tokenCount" label="词数" width="90" />
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column prop="score" label="得分" width="88">
              <template #default="{ row }">
                <span v-if="row.score !== null && row.score !== undefined">{{ row.score }}</span>
                <el-tag v-else type="info" size="small">未评</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewedAt" label="评阅时间" width="180">
              <template #default="{ row }">
                {{ row.reviewedAt || "-" }}
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="评语" min-width="220" show-overflow-tooltip />
            <el-table-column prop="autoComment" label="建议评语" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openReview(row)">
                  {{ row.score === null || row.score === undefined ? "评阅" : "修改" }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无可评阅的提交记录" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="提交评阅" width="700px">
      <el-form :model="reviewForm" label-width="88px">
        <el-form-item label="提交ID">
          <el-input v-model="reviewForm.submissionId" disabled />
        </el-form-item>
        <el-form-item label="学生ID">
          <el-input v-model="reviewForm.studentId" disabled />
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="reviewForm.score" :min="0" :max="100" :step="1" :precision="2" />
          <el-button size="small" style="margin-left: 8px" @click="fillSuggestion">生成建议</el-button>
        </el-form-item>

        <el-divider>维度评分</el-divider>
        <el-table :data="reviewForm.dimensionScores" border size="small">
          <el-table-column prop="dimension" label="维度" width="160" />
          <el-table-column prop="score" label="分数" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :max="100" :step="1" :precision="2" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重(%)" width="110" />
          <el-table-column prop="comment" label="维度评语">
            <template #default="{ row }">
              <el-input v-model="row.comment" size="small" />
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="评语" style="margin-top: 12px">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="routeFromDashboard" :loading="saving" @click="saveReviewAndBackToDashboard">保存并返回工作台</el-button>
        <el-button type="primary" :loading="saving" @click="saveReview">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rubricVisible" title="Rubric配置" width="760px">
      <div class="rubric-chart">
        <div v-for="item in rubricChartItems" :key="`chart-${item.dimension}`" class="rubric-chart-row">
          <div class="rubric-chart-label">{{ item.dimension }}</div>
          <div class="rubric-chart-bar">
            <span class="rubric-chart-fill" :style="{ width: `${item.percent}%` }"></span>
          </div>
          <div class="rubric-chart-value">{{ item.weight }}%</div>
        </div>
      </div>
      <el-table :data="rubricItems" border>
        <el-table-column prop="dimension" label="维度" width="160">
          <template #default="{ row }">
            <el-input v-model="row.dimension" />
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重(%)" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="1" :max="100" :step="1" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述">
          <template #default="{ row }">
            <el-input v-model="row.description" />
          </template>
        </el-table-column>
      </el-table>
      <div class="toolbar-left" style="margin-top: 12px">
        <el-button @click="addRubricItem">新增维度</el-button>
        <el-button type="primary" :loading="savingRubric" @click="saveRubric">保存Rubric</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量打分" width="520px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        将对选中的 {{ selectedRows.length }} 条提交统一打分。
      </el-alert>
      <el-form :model="batchForm" label-width="88px">
        <el-form-item label="统一分数">
          <el-input-number v-model="batchForm.score" :min="0" :max="100" :step="1" :precision="2" />
        </el-form-item>
        <el-form-item label="统一评语">
          <el-input v-model="batchForm.comment" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="saveBatchReview">确认批量打分</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import {
  batchUpsertReviewApi,
  exportSubmissionReviewsCsvApi,
  listSubmissionReviewsApi,
  reviewRubricApi,
  reviewSuggestionApi,
  submissionReviewSummaryApi,
  upsertReviewRubricApi,
  upsertSubmissionReviewApi,
} from "../api/modules";
import { readOptionalPositiveIntQuery, readPositiveIntQuery, readStringQuery } from "../router/query";
import { downloadCsv } from "../utils/download";
import { notifyApiError } from "../utils/notify";
import { buildDashboardReturnQuery, isFromDashboard } from "../router/dashboard-context";
import {
  buildDimensionScores,
  filterReviewRows,
  resolveWeightedScore,
  type DimensionScore,
  type ReviewRow,
  type RubricItem,
} from "./reviews.logic";

const assignmentId = ref(1);
const routeAssignmentId = ref<number | null>(null);
const routeFromDashboard = ref(false);
const pendingOnlyMode = ref(false);
const rows = ref<ReviewRow[]>([]);
const dialogVisible = ref(false);
const saving = ref(false);
const rubricVisible = ref(false);
const savingRubric = ref(false);
const rubricItems = ref<RubricItem[]>([]);
const selectedRows = ref<ReviewRow[]>([]);
const batchDialogVisible = ref(false);
const batchSaving = ref(false);
const batchForm = reactive({
  score: 60,
  comment: "",
});
const route = useRoute();
const router = useRouter();

const summary = reactive({
  totalSubmissions: 0,
  reviewedSubmissions: 0,
  reviewedRate: 0,
  averageScore: 0,
  passCount: 0,
  passRate: 0,
});

const normalizeRate = (value: number) => {
  const numeric = Number(value || 0);
  return numeric <= 1 ? numeric * 100 : numeric;
};

const formatPercent = (value: number) => `${Number(value || 0).toFixed(2)}%`;
const summaryReviewedRate = computed(() => normalizeRate(summary.reviewedRate));
const summaryPassRate = computed(() => normalizeRate(summary.passRate));
const summaryAverageScore = computed(() => Number((Number(summary.averageScore || 0)).toFixed(2)));

const displayRows = computed(() => filterReviewRows(rows.value, pendingOnlyMode.value));
const rubricChartItems = computed(() => {
  const total = rubricItems.value.reduce((sum, item) => sum + Number(item.weight || 0), 0);
  return rubricItems.value.map((item) => {
    const weight = Number(item.weight || 0);
    const percent = total > 0 ? Number(((weight / total) * 100).toFixed(2)) : 0;
    return {
      dimension: item.dimension || "-",
      weight,
      percent,
    };
  });
});

const reviewForm = reactive({
  submissionId: 0,
  studentId: 0,
  score: 60,
  comment: "",
  dimensionScores: [] as DimensionScore[],
});

const loadSummary = async () => {
  const res = await submissionReviewSummaryApi(assignmentId.value);
  summary.totalSubmissions = res.data.totalSubmissions || 0;
  summary.reviewedSubmissions = res.data.reviewedSubmissions || 0;
  summary.reviewedRate = Number(res.data.reviewedRate || 0);
  summary.averageScore = Number(res.data.averageScore || 0);
  summary.passCount = res.data.passCount || 0;
  summary.passRate = Number(res.data.passRate || 0);
};

const loadRubric = async () => {
  const res = await reviewRubricApi(assignmentId.value);
  rubricItems.value = (res.data.items || []).map((item: any) => ({
    dimension: item.dimension,
    weight: Number(item.weight || 0),
    description: item.description || "",
  }));
};

const loadRows = async () => {
  const res = await listSubmissionReviewsApi(assignmentId.value);
  rows.value = Array.isArray(res.data) ? res.data : [];
};

const reloadAll = async () => {
  try {
    await Promise.all([loadSummary(), loadRows(), loadRubric()]);
  } catch (error) {
    notifyApiError(error, "加载评阅数据失败");
  }
};

const openReview = (row: ReviewRow) => {
  reviewForm.submissionId = row.submissionId;
  reviewForm.studentId = row.studentId;
  reviewForm.score = row.score ?? 60;
  reviewForm.comment = row.comment ?? row.autoComment ?? "";

  reviewForm.dimensionScores = buildDimensionScores(rubricItems.value, row.dimensionScoresJson);
  dialogVisible.value = true;
};

const fillSuggestion = async () => {
  try {
    const res = await reviewSuggestionApi({ assignmentId: assignmentId.value, score: reviewForm.score });
    if (!reviewForm.comment.trim()) {
      reviewForm.comment = res.data.suggestion || "";
    }
  } catch (error) {
    notifyApiError(error, "生成建议失败");
  }
};

const saveReview = async (options?: { backToDashboard?: boolean }) => {
  if (!reviewForm.submissionId) {
    ElMessage.warning("请先选择提交记录");
    return;
  }
  saving.value = true;
  try {
    if (reviewForm.dimensionScores.length > 0) {
      const weightedScore = resolveWeightedScore(reviewForm.dimensionScores);
      if (weightedScore !== null) {
        reviewForm.score = weightedScore;
      }
    }

    await upsertSubmissionReviewApi({
      submissionId: reviewForm.submissionId,
      score: reviewForm.score,
      comment: reviewForm.comment,
      dimensionScores: reviewForm.dimensionScores.map((item) => ({
        dimension: item.dimension,
        score: item.score,
        comment: item.comment,
      })),
    });
    dialogVisible.value = false;
    if (options?.backToDashboard && routeFromDashboard.value) {
      backToDashboard({
        handled: "review_saved",
        handledCount: 1,
      });
      return;
    }
    ElMessage.success("评阅已保存");
    await reloadAll();
  } catch (error) {
    notifyApiError(error, "保存评阅失败");
  } finally {
    saving.value = false;
  }
};

const openRubricDialog = async () => {
  rubricVisible.value = true;
  if (rubricItems.value.length === 0) {
    await loadRubric();
  }
};

const addRubricItem = () => {
  rubricItems.value.push({
    dimension: "新维度",
    weight: 10,
    description: "",
  });
};

const saveRubric = async () => {
  savingRubric.value = true;
  try {
    const filtered = rubricItems.value.filter((item) => item.dimension && item.weight > 0);
    if (filtered.length === 0) {
      ElMessage.warning("请至少配置一个有效评分维度");
      return;
    }
    await upsertReviewRubricApi({
      assignmentId: assignmentId.value,
      items: filtered,
    });
    ElMessage.success("Rubric保存成功");
    rubricVisible.value = false;
    await reloadAll();
  } catch (error) {
    notifyApiError(error, "保存Rubric失败");
  } finally {
    savingRubric.value = false;
  }
};

const exportReviewCsv = async () => {
  try {
    const res = await exportSubmissionReviewsCsvApi(assignmentId.value);
    downloadCsv(res.data, `assignment_${assignmentId.value}_reviews.csv`);
  } catch (error) {
    notifyApiError(error, "导出评阅CSV失败");
  }
};

const switchToAllRows = () => {
  router.push({
    path: "/reviews",
    query: {
      ...route.query,
      pendingOnly: undefined,
    },
  });
};

const handleSelectionChange = (selection: ReviewRow[]) => {
  selectedRows.value = selection;
};

const openBatchDialog = () => {
  batchForm.score = 60;
  batchForm.comment = "";
  batchDialogVisible.value = true;
};

const saveBatchReview = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning("请先选择需要打分的提交记录");
    return;
  }
  batchSaving.value = true;
  try {
    const reviews = selectedRows.value.map((row) => ({
      submissionId: row.submissionId,
      score: batchForm.score,
      comment: batchForm.comment || undefined,
    }));
    await batchUpsertReviewApi(reviews);
    batchDialogVisible.value = false;
    ElMessage.success(`已批量评阅 ${reviews.length} 条提交`);
    await reloadAll();
  } catch (error) {
    notifyApiError(error, "批量打分失败");
  } finally {
    batchSaving.value = false;
  }
};

const saveReviewAndBackToDashboard = async () => {
  await saveReview({ backToDashboard: true });
};

const backToDashboard = (options?: { handled?: string; handledCount?: number }) => {
  router.push({
    path: "/dashboard",
    query: buildDashboardReturnQuery({
      focusAssignmentId: assignmentId.value,
      handled: options?.handled,
      handledCount: options?.handledCount,
    }),
  });
};

watch([() => route.query.assignmentId, () => route.query.pendingOnly, () => route.query.from], async () => {
  routeAssignmentId.value = readOptionalPositiveIntQuery(route.query.assignmentId);
  routeFromDashboard.value = isFromDashboard(route.query);
  pendingOnlyMode.value = readStringQuery(route.query.pendingOnly) === "1";
  assignmentId.value = readPositiveIntQuery(route.query.assignmentId, assignmentId.value);
  await reloadAll();
}, { immediate: true });
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 0 4px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 12px;
  color: #6b8f82;
  font-weight: 500;
}

.filter-divider {
  margin: 10px 0 12px;
  border-color: #d1fae5;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  padding: 8px 0;
}

.rubric-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0 12px;
}

.rubric-chart-row {
  display: grid;
  grid-template-columns: 140px 1fr 70px;
  gap: 12px;
  align-items: center;
}

.rubric-chart-label {
  font-size: 13px;
  color: #1a2e26;
  font-weight: 500;
}

.rubric-chart-bar {
  height: 10px;
  background: #e6f7f3;
  border-radius: 999px;
  overflow: hidden;
}

.rubric-chart-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #16a37f, #2563eb);
  border-radius: 999px;
}

.rubric-chart-value {
  text-align: right;
  font-size: 12px;
  color: #16a37f;
  font-weight: 600;
}

:deep(.el-card) {
  border-radius: 14px;
  border-color: #d1fae5;
  box-shadow: 0 2px 12px rgba(22, 163, 127, 0.07);
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, #f0fdf8, #e6f7f3);
  border-bottom-color: #d1fae5;
}

:deep(.el-table th) {
  background: #f0fdf8 !important;
  color: #1a2e26 !important;
  font-weight: 600;
}
</style>
