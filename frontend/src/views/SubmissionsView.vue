<template>
  <AppShell title="作业提交">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>新建提交</span>
          </template>

          <el-form :model="form" label-width="96px">
            <el-form-item label="作业ID">
              <el-input-number v-model="form.assignmentId" :min="1" />
            </el-form-item>
            <el-form-item label="学生ID">
              <el-input-number v-model="form.studentId" :min="1" :disabled="isStudent" />
              <div class="hint">
                {{ isStudent ? "学生身份会自动绑定到当前登录账号" : "教师或管理员可代学生补录提交" }}
              </div>
            </el-form-item>

            <el-form-item label="提交方式">
              <el-radio-group v-model="submitMode">
                <el-radio value="file">文件上传</el-radio>
                <el-radio value="text">在线文本</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="submitMode === 'file'" label="选择文件">
              <input type="file" accept=".txt,.doc,.docx" @change="onFileChange" />
              <div class="hint">支持 txt/doc/docx，提交后会自动生成新版本。</div>
            </el-form-item>

            <el-form-item v-else label="作业文本">
              <el-input
                v-model="form.rawText"
                type="textarea"
                :rows="6"
                placeholder="请输入作业文本"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="header-row">
              <div class="header-left">
                <span>提交记录与反馈</span>
                <el-input-number v-model="queryAssignmentId" :min="1" size="small" />
                <el-button size="small" @click="loadPageData">查询</el-button>
                <el-input-number v-model="evolutionStudentId" :min="1" size="small" :disabled="isStudent" />
                <el-button size="small" type="primary" plain @click="openEvolution">查看演化</el-button>
                <el-tag v-if="focusAssignmentId" type="primary">已定位作业 #{{ focusAssignmentId }}</el-tag>
              </div>
            </div>
          </template>

          <div class="submission-overview">
            <div class="summary-tags">
              <el-tag type="info">记录 {{ feedbackSummary.total }}</el-tag>
              <el-tag type="success">已评阅 {{ feedbackSummary.reviewed }}</el-tag>
              <el-tag type="warning">待反馈 {{ feedbackSummary.pending }}</el-tag>
            </div>
            <div v-if="feedbackSummary.latest" class="summary-latest">
              最新反馈：v{{ feedbackSummary.latest.versionNo }} · 得分 {{ feedbackSummary.latest.score }} ·
              {{ feedbackSummary.latest.comment || feedbackSummary.latest.autoComment || "已收到教师反馈" }}
            </div>
            <div v-else class="summary-latest">当前还没有评阅反馈，提交后可在这里查看最新状态。</div>
          </div>

          <el-table :data="displayRows" border>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="studentId" label="学生ID" width="90" />
            <el-table-column label="版本" width="90">
              <template #default="{ row }">
                <el-tag :type="row.versionNo === latestVersionNo ? 'primary' : 'info'" size="small">v{{ row.versionNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sourceType" label="来源" width="80">
              <template #default="{ row }">
                {{ row.sourceType === 1 ? "文件" : "文本" }}
              </template>
            </el-table-column>
            <el-table-column prop="tokenCount" label="词数" width="90" />
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="反馈状态" width="110">
              <template #default="{ row }">
                <el-tag :type="reviewStatusTagType(row.reviewStatus)" size="small">
                  {{ row.reviewStatus === "reviewed" ? "已评阅" : "待反馈" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="得分" width="90">
              <template #default="{ row }">
                {{ row.score === null ? "-" : row.score }}
              </template>
            </el-table-column>
            <el-table-column label="评阅时间" width="180">
              <template #default="{ row }">
                {{ row.reviewedAt || "-" }}
              </template>
            </el-table-column>
            <el-table-column label="反馈" min-width="240">
              <template #default="{ row }">
                <div class="feedback-text">{{ row.comment || row.autoComment || "暂未生成反馈" }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="evolutionVisible" title="版本演化分析" width="860px">
      <div class="hint" style="margin-bottom: 10px">
        作业ID：{{ queryAssignmentId }}，学生ID：{{ evolutionStudentId }}
      </div>
      <div class="evolution-metrics">
        <el-tag type="info">最新得分 {{ evolutionMetrics.latestScore }}</el-tag>
        <el-tag type="success">平均得分 {{ evolutionMetrics.avgScore }}</el-tag>
        <el-tag type="warning">平均改动率 {{ evolutionMetrics.avgChangeRate }}%</el-tag>
      </div>
      <div v-if="evolutionRows.length > 0" class="evolution-chart-wrap">
        <svg :viewBox="`0 0 ${evolutionChartWidth} ${evolutionChartHeight}`" class="evolution-chart" role="img">
          <line
            :x1="evolutionChartPadding.left"
            :y1="evolutionChartPadding.top"
            :x2="evolutionChartPadding.left"
            :y2="evolutionChartHeight - evolutionChartPadding.bottom"
            stroke="#c0c4cc"
          />
          <line
            :x1="evolutionChartPadding.left"
            :y1="evolutionChartHeight - evolutionChartPadding.bottom"
            :x2="evolutionChartWidth - evolutionChartPadding.right"
            :y2="evolutionChartHeight - evolutionChartPadding.bottom"
            stroke="#c0c4cc"
          />

          <g v-for="tick in evolutionChartData.yTicks" :key="tick.value">
            <line
              :x1="evolutionChartPadding.left"
              :y1="tick.y"
              :x2="evolutionChartWidth - evolutionChartPadding.right"
              :y2="tick.y"
              stroke="#ebeef5"
            />
            <text :x="evolutionChartPadding.left - 8" :y="tick.y + 4" text-anchor="end" fill="#909399" font-size="12">
              {{ tick.value }}
            </text>
          </g>

          <polyline
            v-if="evolutionChartData.scorePoints.length > 1"
            :points="evolutionChartData.scorePolyline"
            fill="none"
            stroke="#409eff"
            stroke-width="2.5"
          />
          <polyline
            v-if="evolutionChartData.changePoints.length > 1"
            :points="evolutionChartData.changePolyline"
            fill="none"
            stroke="#f59e0b"
            stroke-width="2.5"
          />

          <g v-for="point in evolutionChartData.scorePoints" :key="`score-${point.label}-${point.x}`">
            <circle :cx="point.x" :cy="point.y" r="4.5" fill="#409eff" />
            <circle :cx="point.x" :cy="point.y" r="2" fill="#ffffff" />
            <title>{{ point.label }} 得分 {{ point.value }}</title>
          </g>
          <g v-for="point in evolutionChartData.changePoints" :key="`change-${point.label}-${point.x}`">
            <circle :cx="point.x" :cy="point.y" r="4.5" fill="#f59e0b" />
            <circle :cx="point.x" :cy="point.y" r="2" fill="#ffffff" />
            <title>{{ point.label }} 改动率 {{ point.value }}%</title>
          </g>

          <text
            v-for="point in evolutionLabelPoints"
            :key="`label-${point.label}-${point.x}`"
            :x="point.x"
            :y="evolutionChartHeight - evolutionChartPadding.bottom + 16"
            text-anchor="middle"
            fill="#909399"
            font-size="11"
          >
            {{ point.label }}
          </text>
        </svg>
        <div class="evolution-legend">
          <span class="legend-item"><i class="legend-dot legend-dot--score"></i>得分</span>
          <span class="legend-item"><i class="legend-dot legend-dot--change"></i>改动率(%)</span>
        </div>
      </div>
      <el-empty v-else description="暂无演化数据" />
      <el-table :data="evolutionRows" border>
        <el-table-column prop="submissionId" label="提交ID" width="90" />
        <el-table-column prop="versionNo" label="版本" width="80" />
        <el-table-column prop="tokenCount" label="词数" width="90" />
        <el-table-column prop="similarityToPrevious" label="与上一版相似度" width="140" />
        <el-table-column prop="changeRate" label="改动率" width="100" />
        <el-table-column prop="score" label="得分" width="90" />
        <el-table-column prop="reviewedAt" label="评阅时间" width="170" />
        <el-table-column prop="minutesSincePrevious" label="间隔分钟" width="100" />
        <el-table-column prop="submitTime" label="提交时间" />
      </el-table>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute } from "vue-router";
import AppShell from "../components/AppShell.vue";
import { useAuthStore } from "../stores/auth";
import {
  createSubmissionApi,
  listSubmissionReviewsApi,
  listSubmissionsApi,
  submissionEvolutionApi,
  uploadFileApi,
} from "../api/modules";
import { readOptionalPositiveIntQuery, readPositiveIntQuery, readStringQuery } from "../router/query";
import { notifyApiError } from "../utils/notify";
import {
  buildEvolutionChartData,
  buildFeedbackSummary,
  buildSubmissionDisplayRows,
  resolveLatestVersionNo,
  resolveReviewStatusTagType,
  validateSubmissionInput,
  type ReviewRow,
  type SubmissionDisplayRow,
  type SubmissionRow,
} from "./submissions.logic";

type EvolutionRow = {
  submissionId: number;
  versionNo: number;
  tokenCount: number;
  similarityToPrevious: number;
  changeRate: number;
  minutesSincePrevious: number;
  submitTime: string;
  score?: number | null;
  reviewedAt?: string;
};

const authStore = useAuthStore();
const route = useRoute();
const submitMode = ref<"file" | "text">("file");
const submitting = ref(false);
const selectedFile = ref<File | null>(null);
const queryAssignmentId = ref(1);
const focusAssignmentId = ref<number | null>(null);
const tableData = ref<SubmissionRow[]>([]);
const reviewRows = ref<ReviewRow[]>([]);
const evolutionStudentId = ref(3);
const evolutionVisible = ref(false);
const evolutionRows = ref<EvolutionRow[]>([]);
const evolutionChartWidth = 760;
const evolutionChartHeight = 220;
const evolutionChartPadding = {
  left: 50,
  right: 20,
  top: 16,
  bottom: 30,
};
const isStudent = computed(() => (authStore.user?.role || "").trim().toUpperCase() === "STUDENT");

const form = reactive({
  assignmentId: 1,
  studentId: 3,
  rawText: "",
});

const displayRows = computed<SubmissionDisplayRow[]>(() => buildSubmissionDisplayRows(tableData.value, reviewRows.value));
const latestVersionNo = computed(() => resolveLatestVersionNo(displayRows.value));
const feedbackSummary = computed(() => buildFeedbackSummary(displayRows.value));
const evolutionChartData = computed(() =>
  buildEvolutionChartData(evolutionRows.value, evolutionChartWidth, evolutionChartHeight, evolutionChartPadding)
);
const evolutionLabelPoints = computed(() => evolutionChartData.value.scorePoints.filter((point) => point.showLabel));
const evolutionMetrics = computed(() => {
  if (evolutionRows.value.length === 0) {
    return { latestScore: "-", avgScore: "-", avgChangeRate: "-" };
  }
  const scoreRows = evolutionRows.value.filter((row) => row.score !== null && row.score !== undefined);
  const latestScore = scoreRows.length > 0 ? scoreRows[scoreRows.length - 1].score ?? 0 : null;
  const avgScore =
    scoreRows.length > 0
      ? (
          scoreRows.reduce((sum, row) => sum + Number(row.score || 0), 0) / scoreRows.length
        ).toFixed(2)
      : null;
  const avgChangeRate =
    evolutionRows.value.length > 0
      ? ((evolutionRows.value.reduce((sum, row) => sum + Number(row.changeRate || 0), 0) / evolutionRows.value.length) * 100).toFixed(2)
      : null;
  return {
    latestScore: latestScore === null ? "-" : latestScore,
    avgScore: avgScore ?? "-",
    avgChangeRate: avgChangeRate ?? "-",
  };
});

const syncActorContext = () => {
  if (isStudent.value && authStore.user?.id) {
    form.studentId = authStore.user.id;
    evolutionStudentId.value = authStore.user.id;
  }
};

const onFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  selectedFile.value = target.files && target.files.length > 0 ? target.files[0] : null;
};

const reviewStatusTagType = (status: "reviewed" | "pending") => resolveReviewStatusTagType(status);

const submit = async () => {
  submitting.value = true;
  try {
    syncActorContext();
    const validationMessage = validateSubmissionInput({
      mode: submitMode.value,
      file: selectedFile.value,
      rawText: form.rawText,
      assignmentId: form.assignmentId,
      studentId: form.studentId,
    });
    if (validationMessage) {
      ElMessage.warning(validationMessage);
      return;
    }
    if (submitMode.value === "file") {
      const uploadRes = await uploadFileApi(selectedFile.value);
      await createSubmissionApi({
        assignmentId: form.assignmentId,
        studentId: form.studentId,
        fileId: uploadRes.data.fileId,
      });
    } else {
      await createSubmissionApi({
        assignmentId: form.assignmentId,
        studentId: form.studentId,
        rawText: form.rawText,
      });
    }

    ElMessage.success("提交成功");
    queryAssignmentId.value = form.assignmentId;
    form.rawText = "";
    selectedFile.value = null;
    await loadPageData();
  } catch (error) {
    notifyApiError(error, "提交失败");
  } finally {
    submitting.value = false;
  }
};

const loadPageData = async () => {
  try {
    const [submissionRes, reviewRes] = await Promise.all([
      listSubmissionsApi(queryAssignmentId.value),
      listSubmissionReviewsApi(queryAssignmentId.value),
    ]);
    tableData.value = Array.isArray(submissionRes.data) ? submissionRes.data : [];
    reviewRows.value = Array.isArray(reviewRes.data) ? reviewRes.data : [];
  } catch (error) {
    notifyApiError(error, "查询提交记录失败");
  }
};

const openEvolution = async () => {
  try {
    syncActorContext();
    const res = await submissionEvolutionApi(queryAssignmentId.value, evolutionStudentId.value);
    evolutionRows.value = Array.isArray(res.data) ? res.data : [];
    evolutionVisible.value = true;
  } catch (error) {
    notifyApiError(error, "加载版本演化失败");
  }
};

const applyRouteQuery = async () => {
  syncActorContext();
  const nextAssignmentId = readPositiveIntQuery(route.query.assignmentId, queryAssignmentId.value);
  const nextStudentId = readOptionalPositiveIntQuery(route.query.studentId);
  focusAssignmentId.value = readOptionalPositiveIntQuery(route.query.focusAssignmentId);
  queryAssignmentId.value = nextAssignmentId;
  form.assignmentId = nextAssignmentId;
  if (!isStudent.value && nextStudentId) {
    evolutionStudentId.value = nextStudentId;
  }
  await loadPageData();
  if (readStringQuery(route.query.openEvolution) === "1") {
    await openEvolution();
  }
};

watch([() => route.query.assignmentId, () => route.query.focusAssignmentId, () => authStore.user?.id], async () => {
  await applyRouteQuery();
}, { immediate: true });
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.submission-overview {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #f8fafc;
}

.summary-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-latest {
  margin-top: 10px;
  color: #344054;
  font-size: 13px;
  line-height: 1.5;
}

.feedback-text {
  color: #344054;
  line-height: 1.5;
}

.evolution-metrics {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.evolution-chart-wrap {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
}

.evolution-chart {
  width: 100%;
  height: 220px;
  display: block;
}

.evolution-legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.legend-dot--score {
  background: #409eff;
}

.legend-dot--change {
  background: #f59e0b;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
}
</style>
