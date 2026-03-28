<template>
  <AppShell title="作业提交">
    <div class="submissions-layout">
      <!-- 左侧：新建提交 -->
      <div class="submit-panel">
        <div class="edu-card">
          <div class="edu-card__header">
            <span class="edu-card__title">新建提交</span>
          </div>
          <div class="edu-card__body">
            <el-form :model="form" label-width="96px">
              <!-- 学生：课程→作业二级下拉；教师/管理员：直接输入作业ID -->
              <template v-if="isStudent">
                <el-form-item label="选择课程">
                  <el-select v-model="selectedCourseId" placeholder="请选择课程" style="width:100%" @change="onCourseChange">
                    <el-option v-for="c in courseList" :key="c.id" :label="c.courseName + ' (' + c.courseCode + ')'" :value="c.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="选择作业">
                  <el-select v-model="form.assignmentId" placeholder="请先选择课程" style="width:100%" :disabled="!selectedCourseId">
                    <el-option v-for="a in assignmentList" :key="a.id" :label="a.title" :value="a.id" />
                  </el-select>
                </el-form-item>
              </template>
              <el-form-item v-else label="作业ID">
                <el-input-number v-model="form.assignmentId" :min="1" class="edu-input-number" />
              </el-form-item>
              <el-form-item label="学生ID">
                <el-input-number v-model="form.studentId" :min="1" :disabled="isStudent" class="edu-input-number" />
                <div class="field-hint">
                  {{ isStudent ? '学生身份会自动绑定到当前登录账号' : '教师或管理员可代学生补录提交' }}
                </div>
              </el-form-item>

              <el-form-item label="提交方式">
                <el-radio-group v-model="submitMode">
                  <el-radio value="file">文件上传</el-radio>
                  <el-radio value="text">在线文本</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="submitMode === 'file'" label="选择文件">
                <div class="file-upload-area">
                  <input type="file" accept=".txt,.doc,.docx" @change="onFileChange" class="file-input" />
                  <div class="field-hint">支持 txt / doc / docx，提交后自动生成新版本</div>
                </div>
              </el-form-item>

              <el-form-item v-else label="作业文本">
                <el-input
                  v-model="form.rawText"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入作业文本内容"
                  class="edu-textarea"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="submit" class="submit-btn">提交作业</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 右侧：提交记录 -->
      <div class="records-panel">
        <div class="edu-card">
          <div class="edu-card__header">
            <span class="edu-card__title">提交记录与反馈</span>
            <div class="header-controls">
              <el-input-number v-model="queryAssignmentId" :min="1" size="small" class="edu-input-number" />
              <el-button size="small" @click="loadPageData">查询</el-button>
              <el-input-number v-model="evolutionStudentId" :min="1" size="small" :disabled="isStudent" class="edu-input-number" />
              <el-button size="small" type="primary" plain @click="openEvolution">版本演化</el-button>
              <el-tag v-if="focusAssignmentId" type="primary" size="small">定位 #{{ focusAssignmentId }}</el-tag>
            </div>
          </div>

          <div class="edu-card__body">
            <div v-if="feedbackSummary" class="feedback-overview">
              <div class="feedback-tags">
                <span class="feedback-chip feedback-chip--blue">已提交 {{ feedbackSummary.totalCount }} 次</span>
                <span class="feedback-chip feedback-chip--green" v-if="feedbackSummary.reviewedCount > 0">已评阅 {{ feedbackSummary.reviewedCount }} 次</span>
                <span class="feedback-chip feedback-chip--amber" v-if="feedbackSummary.pendingCount > 0">待反馈 {{ feedbackSummary.pendingCount }} 次</span>
              </div>
              <div v-if="feedbackSummary.latest" class="feedback-latest">
                最新：v{{ feedbackSummary.latest.versionNo }} · 得分 <strong>{{ feedbackSummary.latest.score }}</strong> ·
                {{ feedbackSummary.latest.comment || feedbackSummary.latest.autoComment || '已收到教师反馈' }}
              </div>
              <div v-else class="feedback-latest">当前还没有评阅反馈，提交后可在这里查看最新状态。</div>
            </div>

            <el-table :data="displayRows" border size="small" class="edu-table">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="studentId" label="学生ID" width="90" />
              <el-table-column label="版本" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.versionNo === latestVersionNo ? 'primary' : 'info'" size="small">v{{ row.versionNo }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sourceType" label="来源" width="80">
                <template #default="{ row }">
                  {{ row.sourceType === 1 ? '文件' : '文本' }}
                </template>
              </el-table-column>
              <el-table-column prop="tokenCount" label="词数" width="90" />
              <el-table-column prop="submitTime" label="提交时间" width="180" />
              <el-table-column label="反馈状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="reviewStatusTagType(row.reviewStatus)" size="small">
                    {{ row.reviewStatus === 'reviewed' ? '已评阅' : '待反馈' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="80">
                <template #default="{ row }">
                  <span :class="row.score !== null ? 'score-value' : 'score-empty'">
                    {{ row.score === null ? '–' : row.score }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="评阅时间" width="160">
                <template #default="{ row }">{{ row.reviewedAt || '–' }}</template>
              </el-table-column>
              <el-table-column label="反馈" min-width="200">
                <template #default="{ row }">
                  <span class="feedback-text">{{ row.comment || row.autoComment || '暂未生成反馈' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </div>

    <!-- 版本演化弹窗 -->
    <el-dialog v-model="evolutionVisible" title="版本演化分析" width="860px">
      <div class="evo-meta">作业 ID：{{ queryAssignmentId }}，学生 ID：{{ evolutionStudentId }}</div>
      <div class="evo-chips">
        <span class="feedback-chip feedback-chip--blue">最新得分 {{ evolutionMetrics.latestScore }}</span>
        <span class="feedback-chip feedback-chip--green">平均得分 {{ evolutionMetrics.avgScore }}</span>
        <span class="feedback-chip feedback-chip--amber">平均改动率 {{ evolutionMetrics.avgChangeRate }}%</span>
      </div>
      <div v-if="evolutionRows.length > 0">
        <div class="evo-chart-wrap">
          <svg class="evo-chart" :viewBox="`0 0 ${evolutionChartData.width} ${evolutionChartHeight}`" preserveAspectRatio="xMidYMid meet">
            <line v-for="i in 4" :key="`grid-${i}`"
              :x1="evolutionChartData.padLeft" :x2="evolutionChartData.width - evolutionChartData.padRight"
              :y1="evolutionChartData.padTop + (i-1) * evolutionChartData.rowH"
              :y2="evolutionChartData.padTop + (i-1) * evolutionChartData.rowH"
              stroke="#d1fae5" stroke-dasharray="4 3" />
            <polyline v-if="evolutionChartData.scoreLine" :points="evolutionChartData.scoreLine" fill="none" stroke="#16a37f" stroke-width="2.5" stroke-linejoin="round" />
            <polyline v-if="evolutionChartData.changeLine" :points="evolutionChartData.changeLine" fill="none" stroke="#f59e0b" stroke-width="2" stroke-dasharray="5 3" stroke-linejoin="round" />
            <g v-for="point in evolutionChartData.scorePoints" :key="`sp-${point.label}`">
              <circle :cx="point.x" :cy="point.y" r="5" fill="#16a37f" />
              <circle :cx="point.x" :cy="point.y" r="2.5" fill="#fff" />
            </g>
            <g v-for="point in evolutionChartData.changePoints" :key="`cp-${point.label}`">
              <circle :cx="point.x" :cy="point.y" r="4.5" fill="#f59e0b" />
              <circle :cx="point.x" :cy="point.y" r="2" fill="#fff" />
            </g>
            <text v-for="point in evolutionLabelPoints" :key="`lbl-${point.label}`"
              :x="point.x" :y="evolutionChartHeight - evolutionChartPadding.bottom + 16"
              text-anchor="middle" fill="#6b8f82" font-size="11">{{ point.label }}</text>
          </svg>
          <div class="evo-legend">
            <span class="legend-item"><i class="legend-dot legend-dot--score"></i>得分</span>
            <span class="legend-item"><i class="legend-dot legend-dot--change"></i>改动率(%)</span>
          </div>
        </div>
        <el-table :data="evolutionRows" border size="small" class="edu-table">
          <el-table-column prop="submissionId" label="提交ID" width="90" />
          <el-table-column prop="versionNo" label="版本" width="80" />
          <el-table-column prop="tokenCount" label="词数" width="90" />
          <el-table-column prop="similarityToPrevious" label="与上版相似度" width="130" />
          <el-table-column prop="changeRate" label="改动率" width="100" />
          <el-table-column prop="score" label="得分" width="90" />
          <el-table-column prop="reviewedAt" label="评阅时间" width="170" />
          <el-table-column prop="minutesSincePrevious" label="间隔(分)" width="100" />
          <el-table-column prop="submitTime" label="提交时间" />
        </el-table>
      </div>
      <el-empty v-else description="暂无演化数据" />
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onMounted as _onMounted } from "vue";
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
import { listCoursesApi } from "../api/courses";
import { listAssignmentsApi } from "../api/assignments";
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
const evolutionStudentId = ref(1);
const evolutionVisible = ref(false);
const evolutionRows = ref<EvolutionRow[]>([]);
const tableData = ref<SubmissionRow[]>([]);
const reviewRows = ref<ReviewRow[]>([]);
const evolutionChartHeight = 220;
const evolutionChartPadding = { top: 20, bottom: 24, left: 48, right: 20 };

const isStudent = computed(() => authStore.user?.role === "student");

// 学生课程→作业联动
const courseList = ref<any[]>([]);
const assignmentList = ref<any[]>([]);
const selectedCourseId = ref<number | null>(null);

const onCourseChange = async (courseId: number) => {
  assignmentList.value = [];
  form.assignmentId = 1;
  if (!courseId) return;
  try {
    const res = await listAssignmentsApi(courseId);
    assignmentList.value = Array.isArray(res.data) ? res.data : [];
    if (assignmentList.value.length > 0) {
      form.assignmentId = assignmentList.value[0].id;
    }
  } catch (error) {
    notifyApiError(error, "加载作业列表失败");
  }
};

const loadCourses = async () => {
  try {
    const res = await listCoursesApi();
    courseList.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    notifyApiError(error, "加载课程列表失败");
  }
};

const form = reactive({
  assignmentId: 1,
  studentId: authStore.user?.id ?? 1,
  rawText: "",
});

const syncActorContext = () => {
  if (isStudent.value && authStore.user?.id) {
    form.studentId = authStore.user.id;
    evolutionStudentId.value = authStore.user.id;
  }
};

const displayRows = computed(() =>
  buildSubmissionDisplayRows(tableData.value, reviewRows.value)
);
const latestVersionNo = computed(() => resolveLatestVersionNo(tableData.value));
const feedbackSummary = computed(() => buildFeedbackSummary(displayRows.value));
const reviewStatusTagType = resolveReviewStatusTagType;

const evolutionChartData = computed(() =>
  buildEvolutionChartData(evolutionRows.value, {
    width: 800,
    height: evolutionChartHeight,
    padding: evolutionChartPadding,
  })
);

const evolutionLabelPoints = computed(() => evolutionChartData.value.scorePoints);
const evolutionMetrics = computed(() => {
  if (!evolutionRows.value.length) return { latestScore: "–", avgScore: "–", avgChangeRate: "–" };
  const scores = evolutionRows.value.map((r) => r.score ?? 0).filter(Boolean);
  const changes = evolutionRows.value.map((r) => r.changeRate ?? 0);
  const avg = (arr: number[]) =>
    arr.length ? (arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(1) : "–";
  return {
    latestScore: evolutionRows.value.at(-1)?.score ?? "–",
    avgScore: avg(scores),
    avgChangeRate: avg(changes),
  };
});

const onFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  selectedFile.value = target.files?.[0] ?? null;
};

const submit = async () => {
  syncActorContext();
  const errMsg = validateSubmissionInput(submitMode.value, form, selectedFile.value);
  if (errMsg) { ElMessage.warning(errMsg); return; }
  submitting.value = true;
  try {
    if (submitMode.value === "file" && selectedFile.value) {
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

// 学生登录后自动加载课程列表
watch(isStudent, (val) => {
  if (val) loadCourses();
}, { immediate: true });
</script>

<style scoped>
.submissions-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  align-items: start;
}

.edu-card {
  background: #ffffff;
  border: 1px solid #d1fae5;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(22, 163, 127, 0.07);
}

.edu-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f0fdf8 0%, #e6f7f3 100%);
  border-bottom: 1px solid #d1fae5;
}

.edu-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #1a2e26;
  letter-spacing: 0.3px;
}

.edu-card__body {
  padding: 20px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6b8f82;
  line-height: 1.4;
}

.file-upload-area {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-input {
  padding: 6px 0;
  font-size: 13px;
  color: #1a2e26;
}

.submit-btn {
  background: linear-gradient(135deg, #16a37f, #059669);
  border: none;
  border-radius: 8px;
  font-weight: 600;
  padding: 10px 28px;
  height: auto;
}

.feedback-overview {
  margin-bottom: 14px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f0fdf8, #fafffe);
  border: 1px solid #d1fae5;
  border-radius: 12px;
}

.feedback-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.feedback-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
}

.feedback-chip--blue  { background: #eff6ff; color: #2563eb; }
.feedback-chip--green { background: #f0fdf4; color: #16a34a; }
.feedback-chip--amber { background: #fffbeb; color: #d97706; }

.feedback-latest {
  font-size: 13px;
  color: #344054;
  line-height: 1.5;
}

.feedback-text {
  font-size: 12px;
  color: #344054;
  line-height: 1.5;
}

.score-value {
  font-weight: 700;
  color: #16a37f;
}

.score-empty {
  color: #94a3b8;
}

.evo-meta {
  font-size: 13px;
  color: #6b8f82;
  margin-bottom: 10px;
}

.evo-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.evo-chart-wrap {
  margin-bottom: 16px;
  border: 1px solid #d1fae5;
  border-radius: 12px;
  padding: 12px;
  background: #fafffe;
}

.evo-chart {
  width: 100%;
  height: 220px;
  display: block;
}

.evo-legend {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #6b8f82;
  margin-top: 8px;
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

.legend-dot--score  { background: #16a37f; }
.legend-dot--change { background: #f59e0b; }

.edu-table :deep(th) {
  background: #f0fdf8 !important;
  color: #1a2e26 !important;
  font-weight: 600;
}

.edu-input-number {
  width: 110px;
}

@media (max-width: 900px) {
  .submissions-layout {
    grid-template-columns: 1fr;
  }
}
</style>