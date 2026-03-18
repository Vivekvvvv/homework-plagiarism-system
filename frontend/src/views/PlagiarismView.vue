<template>
  <AppShell title="查重任务">
    <el-row :gutter="16">
      <el-col :span="9">
        <el-card>
          <template #header>
            <span>创建任务</span>
          </template>
          <el-form :model="form" label-width="110px">
            <el-form-item label="作业ID">
              <el-input-number v-model="form.assignmentId" :min="1" />
            </el-form-item>
            <el-form-item label="阈值">
              <el-input-number v-model="form.threshold" :min="0.1" :max="1" :step="0.01" :precision="2" />
            </el-form-item>
            <el-form-item label="SimHash权重">
              <el-input-number v-model="form.simhashWeight" :min="0" :max="1" :step="0.05" :precision="2" />
            </el-form-item>
            <el-form-item label="Jaccard权重">
              <el-input-number v-model="form.jaccardWeight" :min="0" :max="1" :step="0.05" :precision="2" />
            </el-form-item>
            <el-form-item label="最大重试">
              <el-input-number v-model="form.maxRetry" :min="0" :max="5" :step="1" />
            </el-form-item>
            <el-form-item label="超时(秒)">
              <el-input-number v-model="form.runTimeoutSeconds" :min="30" :max="900" :step="30" />
            </el-form-item>
            <el-form-item label="幂等键">
              <el-input v-model="form.idempotencyKey" placeholder="可选，用于去重" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="creating" @click="createTask">开始检测</el-button>
              <el-button v-if="routeFromDashboard" :loading="creating" @click="createTaskAndBackToDashboard">发起并返回工作台</el-button>
              <el-button @click="loadTasks">刷新</el-button>
              <el-button :disabled="!canCancelTask" type="warning" plain @click="cancelCurrentTask">取消</el-button>
              <el-button :disabled="!canRetryTask" type="primary" plain @click="retryCurrentTask">重试</el-button>
              <el-button v-if="routeFromDashboard" link type="primary" @click="backToDashboard">返回工作台</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>任务列表</span>
          </template>
          <el-table :data="taskTable" height="380" @row-click="selectTask">
            <el-table-column prop="id" label="任务ID" width="86" />
            <el-table-column prop="status" label="状态" width="88">
              <template #default="{ row }">
                {{ statusText(row.status) }}
              </template>
            </el-table-column>
            <el-table-column prop="threshold" label="阈值" width="100" />
            <el-table-column prop="totalPairs" label="配对数" width="80" />
            <el-table-column prop="highRiskPairs" label="高风险" width="80" />
            <el-table-column prop="errorMessage" label="错误信息">
              <template #default="{ row }">
                <el-text v-if="row.errorMessage" type="danger" truncated>{{ row.errorMessage }}</el-text>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card style="margin-bottom: 16px">
          <template #header>
            <span>报告快照</span>
          </template>

          <el-empty v-if="!taskReport" description="选择一个任务查看报告快照" />
          <template v-else>
            <el-row :gutter="12">
              <el-col :span="6">
                <el-statistic title="总配对数" :value="taskReport.totalPairs || 0" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="高风险" :value="taskReport.highRiskPairs || 0" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="阈值" :value="taskReport.threshold || 0" :precision="4" />
              </el-col>
              <el-col :span="6">
                <div class="status-box">
                  <div class="status-title">状态</div>
                  <StatusTag :type="statusTagType(taskReport.status)" :label="statusText(taskReport.status)" />
                </div>
              </el-col>
            </el-row>

            <el-divider />
            <div class="trend-meta">
              <span>算法：{{ taskReport.algorithm || "-" }}</span>
              <span>权重：{{ taskReport.simhashWeight ?? "-" }} / {{ taskReport.jaccardWeight ?? "-" }}</span>
              <span>重试：{{ taskReport.retryCount ?? 0 }} / {{ taskReport.maxRetry ?? 0 }}</span>
              <span>超时：{{ taskReport.runTimeoutSeconds ?? "-" }}秒</span>
            </div>

            <div class="risk-line">
              <span>低风险 ({{ taskReport.lowRiskCount || 0 }})</span>
              <el-progress :percentage="riskPercent(taskReport.lowRiskCount || 0)" status="success" />
            </div>
            <div class="risk-line">
              <span>中风险 ({{ taskReport.mediumRiskCount || 0 }})</span>
              <el-progress :percentage="riskPercent(taskReport.mediumRiskCount || 0)" />
            </div>
            <div class="risk-line">
              <span>高风险 ({{ taskReport.highRiskCount || 0 }})</span>
              <el-progress :percentage="riskPercent(taskReport.highRiskCount || 0)" status="exception" />
            </div>

            <el-divider />

            <el-table :data="taskReport.topPairs || []" height="180" size="small" border>
              <el-table-column prop="id" label="配对ID" width="98" />
              <el-table-column prop="submissionAId" label="提交A" width="72" />
              <el-table-column prop="submissionBId" label="提交B" width="72" />
              <el-table-column prop="similarity" label="相似度" width="98" />
              <el-table-column prop="riskLevel" label="风险" width="82">
                <template #default="{ row }">
                  <StatusTag :type="riskTagType(row.riskLevel)" :label="riskText(row.riskLevel)" />
                </template>
              </el-table-column>
              <el-table-column prop="hammingDistance" label="汉明距离" width="96" />
            </el-table>
          </template>
        </el-card>

        <el-card>
          <template #header>
            <div class="header-row">
              <div class="header-left">
                <span>检测结果（任务 {{ currentTaskId || "-" }}）</span>
                <el-select v-model="filters.riskLevel" clearable placeholder="风险等级" style="width: 110px">
                  <el-option :value="1" label="低风险" />
                  <el-option :value="2" label="中风险" />
                  <el-option :value="3" label="高风险" />
                </el-select>
                <el-input-number
                  v-model="filters.minSimilarity"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  placeholder="最小相似度"
                />
                <el-button :disabled="!currentTaskId" @click="applyFilters">筛选</el-button>
                <el-button :disabled="!currentTaskId" @click="exportCsv">导出CSV</el-button>
                <el-button :disabled="!form.assignmentId" @click="exportAssignmentReport">导出报告</el-button>
              </div>
              <StatusTag
                v-if="currentTaskStatus !== null"
                :type="statusTagType(currentTaskStatus)"
                :label="statusText(currentTaskStatus)"
              />
            </div>
          </template>

          <el-table :data="pairTable" border @row-click="openPairDetail">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="submissionAId" label="提交A" width="80" />
            <el-table-column prop="submissionBId" label="提交B" width="80" />
            <el-table-column prop="similarity" label="相似度" width="110" />
            <el-table-column prop="simhashSimilarity" label="SimHash" width="100" />
            <el-table-column prop="jaccardSimilarity" label="Jaccard" width="100" />
            <el-table-column prop="hammingDistance" label="汉明距离" width="110" />
            <el-table-column prop="riskLevel" label="风险" width="90">
              <template #default="{ row }">
                <StatusTag :type="riskTagType(row.riskLevel)" :label="riskText(row.riskLevel)" />
              </template>
            </el-table-column>
            <el-table-column prop="pairKey" label="配对键" />
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="pairPage.total"
              :current-page="pairPage.pageNo"
              :page-size="pairPage.pageSize"
              :page-sizes="[10, 20, 50]"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>任务日志</span>
          </template>
          <el-table :data="taskLogs" height="220" border>
            <el-table-column prop="createdAt" label="时间" width="180" />
            <el-table-column prop="phase" label="阶段" width="120">
              <template #default="{ row }">
                <StatusTag :type="phaseTagType(row.phase)" :label="phaseText(row.phase)" />
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" />
          </el-table>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <div class="trend-header">
              <span>作业趋势</span>
              <div class="trend-actions">
                <el-button size="small" :disabled="trendRows.length === 0" @click="exportTrendCsv">导出趋势CSV</el-button>
                <el-button size="small" :disabled="trendRows.length === 0" @click="exportTrendPng">导出趋势PNG</el-button>
                <el-button size="small" :disabled="trendRows.length === 0" @click="exportDefensePdf">导出PDF报告</el-button>
              </div>
            </div>
          </template>
          <div class="trend-filter">
            <el-date-picker
              v-model="trendFilter.range"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              :clearable="true"
            />
            <el-input-number v-model="trendFilter.limit" :min="1" :max="200" :step="10" />
            <el-button @click="applyTrendFilter">应用筛选</el-button>
            <el-button @click="resetTrendFilter">重置</el-button>
          </div>
          <div class="trend-meta">
            <span>范围：{{ trendRangeText }}</span>
            <span>限制：{{ trendFilter.limit }}</span>
            <span>数据点：{{ trendRows.length }}</span>
          </div>
          <div v-if="trendRows.length > 0" class="trend-chart-wrap">
            <svg :viewBox="`0 0 ${trendChartWidth} ${trendChartHeight}`" class="trend-chart" role="img">
              <line
                :x1="trendChartPadding.left"
                :y1="trendChartPadding.top"
                :x2="trendChartPadding.left"
                :y2="trendChartHeight - trendChartPadding.bottom"
                stroke="#c0c4cc"
              />
              <line
                :x1="trendChartPadding.left"
                :y1="trendChartHeight - trendChartPadding.bottom"
                :x2="trendChartWidth - trendChartPadding.right"
                :y2="trendChartHeight - trendChartPadding.bottom"
                stroke="#c0c4cc"
              />

              <g v-for="tick in trendChartData.yTicks" :key="tick.value">
                <line
                  :x1="trendChartPadding.left"
                  :y1="tick.y"
                  :x2="trendChartWidth - trendChartPadding.right"
                  :y2="tick.y"
                  stroke="#ebeef5"
                />
                <text :x="trendChartPadding.left - 8" :y="tick.y + 4" text-anchor="end" fill="#909399" font-size="12">
                  {{ tick.value }}%
                </text>
              </g>

              <polyline
                v-if="trendChartData.points.length > 1"
                :points="trendChartData.polyline"
                fill="none"
                stroke="#409eff"
                stroke-width="2.5"
              />

              <g v-for="point in trendChartData.points" :key="`${point.label}-${point.x}`">
                <circle :cx="point.x" :cy="point.y" r="5" fill="#409eff" />
                <circle :cx="point.x" :cy="point.y" r="2.5" fill="#ffffff" />
                <title>{{ point.label }}: {{ point.rate }}%</title>
              </g>

              <text
                v-for="point in trendChartLabelPoints"
                :key="`label-${point.label}-${point.x}`"
                :x="point.x"
                :y="trendChartHeight - trendChartPadding.bottom + 16"
                text-anchor="middle"
                fill="#909399"
                font-size="11"
              >
                {{ point.label }}
              </text>
            </svg>
          </div>
          <el-empty v-else description="暂无趋势数据" />

          <el-table :data="trendRows" height="220" border size="small">
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :type="statusTagType(row.status)" :label="statusText(row.status)" />
              </template>
            </el-table-column>
            <el-table-column prop="totalPairs" label="配对数" width="88" />
            <el-table-column prop="highRiskPairs" label="高风险" width="88" />
            <el-table-column label="高风险率" width="120">
              <template #default="{ row }">
                {{ trendRiskPercent(row.highRiskRate) }}%
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="detailVisible" title="配对详情" size="40%">
      <template v-if="pairDetail">
        <p><strong>配对ID：</strong> {{ pairDetail.id }}</p>
        <p><strong>融合相似度：</strong> {{ pairDetail.similarity }}</p>
        <p><strong>SimHash相似度：</strong> {{ pairDetail.simhashSimilarity ?? "-" }}</p>
        <p><strong>Jaccard相似度：</strong> {{ pairDetail.jaccardSimilarity ?? "-" }}</p>
        <p><strong>汉明距离：</strong> {{ pairDetail.hammingDistance }}</p>
        <p><strong>风险等级：</strong> {{ riskText(pairDetail.riskLevel) }}</p>
        <p><strong>原因：</strong> {{ explainInfo.riskReason || "-" }}</p>
        <p><strong>阈值：</strong> {{ explainInfo.threshold ?? "-" }}</p>
        <p><strong>重叠词元：</strong></p>
        <el-tag v-for="token in explainInfo.overlapTokens || []" :key="`token-${token}`" class="frag-tag">{{ token }}</el-tag>
        <el-divider />
        <p><strong>匹配片段</strong></p>
        <el-empty v-if="matchedFragments.length === 0" description="无匹配片段" />
        <el-tag v-for="item in matchedFragments" :key="item" class="frag-tag">{{ item }}</el-tag>
      </template>
    </el-drawer>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import StatusTag from "../components/StatusTag.vue";
import {
  cancelPlagiarismTaskApi,
  createPlagiarismTaskApi,
  exportAssignmentPlagiarismReportApi,
  exportPlagiarismPairsCsvApi,
  latestPlagiarismTaskApi,
  listPlagiarismPairsApi,
  listPlagiarismTasksApi,
  plagiarismAssignmentTrendApi,
  plagiarismPairDetailApi,
  plagiarismTaskApi,
  plagiarismTaskLogsApi,
  plagiarismTaskReportApi,
  retryPlagiarismTaskApi,
} from "../api/modules";
import { readPositiveIntQuery } from "../router/query";
import { downloadBlob, downloadCsv } from "../utils/download";
import { notifyApiError } from "../utils/notify";
import { buildDashboardReturnQuery, isFromDashboard } from "../router/dashboard-context";
import {
  buildTrendChartData,
  phaseTagType,
  phaseText,
  riskTagType,
  riskText,
  statusTagType,
  statusText,
  trendRiskPercent,
} from "./plagiarism.logic";

type TaskRow = {
  id: number;
  status: number;
  threshold: number;
  simhashWeight?: number;
  jaccardWeight?: number;
  retryCount?: number;
  maxRetry?: number;
  runTimeoutSeconds?: number;
  idempotencyKey?: string;
  totalPairs: number;
  highRiskPairs: number;
  errorMessage?: string;
};

type PairRow = {
  id: number;
  submissionAId: number;
  submissionBId: number;
  pairKey: string;
  similarity: number;
  simhashSimilarity?: number;
  jaccardSimilarity?: number;
  hammingDistance: number;
  riskLevel: number;
  matchedFragmentsJson?: string;
  explainJson?: string;
};

type TaskLogRow = {
  id: number;
  taskId: number;
  phase: string;
  message?: string;
  createdAt: string;
};

type ReportPairRow = {
  id: number;
  submissionAId: number;
  submissionBId: number;
  similarity: number;
  riskLevel: number;
  hammingDistance: number;
};

type TaskReportRow = {
  assignmentId: number;
  taskId: number;
  status: number;
  algorithm?: string;
  threshold: number;
  simhashWeight?: number;
  jaccardWeight?: number;
  totalPairs: number;
  highRiskPairs: number;
  retryCount?: number;
  maxRetry?: number;
  runTimeoutSeconds?: number;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
  lowRiskCount: number;
  mediumRiskCount: number;
  highRiskCount: number;
  topPairs: ReportPairRow[];
};

type TrendRow = {
  taskId: number;
  status: number;
  threshold: number;
  totalPairs: number;
  highRiskPairs: number;
  highRiskRate: number;
  createdAt?: string;
  finishedAt?: string;
};

const creating = ref(false);
const currentTaskId = ref<number | null>(null);
const currentTaskStatus = ref<number | null>(null);
const taskTable = ref<TaskRow[]>([]);
const pairTable = ref<PairRow[]>([]);
const taskLogs = ref<TaskLogRow[]>([]);
const taskReport = ref<TaskReportRow | null>(null);
const trendRows = ref<TrendRow[]>([]);
const detailVisible = ref(false);
const pairDetail = ref<PairRow | null>(null);
const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null);
const route = useRoute();
const router = useRouter();
const routeFromDashboard = ref(false);
const pairPage = reactive({
  total: 0,
  pageNo: 1,
  pageSize: 20,
});

const trendChartWidth = 760;
const trendChartHeight = 220;
const trendChartPadding = {
  left: 50,
  right: 20,
  top: 16,
  bottom: 30,
};

const form = reactive({
  assignmentId: 1,
  threshold: 0.7,
  simhashWeight: 0.7,
  jaccardWeight: 0.3,
  maxRetry: 1,
  runTimeoutSeconds: 120,
  idempotencyKey: "",
});

const trendFilter = reactive<{
  range: string[];
  limit: number;
}>({
  range: [],
  limit: 20,
});

const filters = reactive<{
  riskLevel?: number;
  minSimilarity?: number;
}>({
  riskLevel: undefined,
  minSimilarity: undefined,
});

const resetPairState = () => {
  pairPage.pageNo = 1;
  pairPage.total = 0;
  pairTable.value = [];
};

const resetTaskState = () => {
  currentTaskId.value = null;
  currentTaskStatus.value = null;
  resetPairState();
  taskLogs.value = [];
  taskReport.value = null;
  trendRows.value = [];
};

const matchedFragments = computed(() => {
  if (!pairDetail.value?.matchedFragmentsJson) return [];
  try {
    return JSON.parse(pairDetail.value.matchedFragmentsJson) as string[];
  } catch {
    return [];
  }
});

const explainInfo = computed(() => {
  if (!pairDetail.value?.explainJson) return {} as Record<string, any>;
  try {
    return JSON.parse(pairDetail.value.explainJson) as Record<string, any>;
  } catch {
    return {} as Record<string, any>;
  }
});

const canCancelTask = computed(() => currentTaskStatus.value === 0 || currentTaskStatus.value === 1);
const canRetryTask = computed(() => currentTaskStatus.value === 2 || currentTaskStatus.value === 3 || currentTaskStatus.value === 4);

const reportTotal = computed(() => {
  if (!taskReport.value) return 0;
  return (taskReport.value.lowRiskCount || 0) + (taskReport.value.mediumRiskCount || 0) + (taskReport.value.highRiskCount || 0);
});

const riskPercent = (count: number) => {
  if (!reportTotal.value) return 0;
  return Number(((count / reportTotal.value) * 100).toFixed(1));
};

const trendChartData = computed(() => buildTrendChartData(trendRows.value, trendChartWidth, trendChartHeight, trendChartPadding));

const trendChartLabelPoints = computed(() => {
  return trendChartData.value.points.filter((point) => point.showLabel);
});

const trendRangeText = computed(() => {
  if (trendFilter.range.length === 2) {
    return `${trendFilter.range[0]} ~ ${trendFilter.range[1]}`;
  }
  return "ALL";
});


const startPolling = () => {
  stopPolling();
  if (!currentTaskId.value) return;
  pollingTimer.value = setInterval(async () => {
    if (!currentTaskId.value) return;
    try {
      const res = await plagiarismTaskApi(currentTaskId.value);
      currentTaskStatus.value = res.data.status;
      await loadTaskLogs();
      await loadTaskReport();
      if (res.data.status === 2 || res.data.status === 3 || res.data.status === 4) {
        stopPolling();
        await loadTasks();
        await loadPairs();
        await loadTaskLogs();
        await loadTaskReport();
        if (res.data.status === 3 && res.data.errorMessage) {
          ElMessage.error(res.data.errorMessage);
        }
      }
    } catch {
      stopPolling();
    }
  }, 2000);
};

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
    pollingTimer.value = null;
  }
};

const backToDashboard = (options?: { handled?: string; handledCount?: number }) => {
  router.push({
    path: "/dashboard",
    query: buildDashboardReturnQuery({
      focusAssignmentId: form.assignmentId,
      handled: options?.handled,
      handledCount: options?.handledCount,
    }),
  });
};

const syncAssignmentContext = async () => {
  stopPolling();
  routeFromDashboard.value = isFromDashboard(route.query);
  form.assignmentId = readPositiveIntQuery(route.query.assignmentId, form.assignmentId);
  resetTaskState();

  try {
    const latest = await latestPlagiarismTaskApi(form.assignmentId);
    currentTaskId.value = latest.data.id;
    currentTaskStatus.value = latest.data.status;
  } catch {
    currentTaskId.value = null;
    currentTaskStatus.value = null;
  }

  await loadTasks();
  await loadPairs();
  await loadTaskLogs();
  await loadTaskReport();
  if (currentTaskStatus.value === 0 || currentTaskStatus.value === 1) {
    startPolling();
  }
};

const createTask = async (options?: { backToDashboard?: boolean }) => {
  creating.value = true;
  try {
    const res = await createPlagiarismTaskApi({
      assignmentId: form.assignmentId,
      threshold: form.threshold,
      simhashWeight: form.simhashWeight,
      jaccardWeight: form.jaccardWeight,
      idempotencyKey: form.idempotencyKey || undefined,
      maxRetry: form.maxRetry,
      runTimeoutSeconds: form.runTimeoutSeconds,
    });
    currentTaskId.value = res.data.id;
    currentTaskStatus.value = res.data.status;
    if (options?.backToDashboard && routeFromDashboard.value) {
      backToDashboard({
        handled: "plagiarism_started",
        handledCount: 1,
      });
      return;
    }
    resetPairState();
    ElMessage.success("任务已在后台启动");
    await loadTasks();
    await loadTaskReport();
    startPolling();
  } catch (error) {
    notifyApiError(error, "创建任务失败");
  } finally {
    creating.value = false;
  }
};

const createTaskAndBackToDashboard = async () => {
  await createTask({ backToDashboard: true });
};

const loadTasks = async () => {
  try {
    const res = await listPlagiarismTasksApi(form.assignmentId);
    taskTable.value = res.data;
    if (!currentTaskId.value && taskTable.value.length > 0) {
      currentTaskId.value = taskTable.value[0].id;
    }
    const current = taskTable.value.find((item) => item.id === currentTaskId.value);
    currentTaskStatus.value = current?.status ?? null;
    await loadTrend();
  } catch (error) {
    notifyApiError(error, "加载任务失败");
  }
};

const loadPairs = async () => {
  if (!currentTaskId.value) return;
  try {
    const res = await listPlagiarismPairsApi(currentTaskId.value, {
      riskLevel: filters.riskLevel,
      minSimilarity: filters.minSimilarity,
      pageNo: pairPage.pageNo,
      pageSize: pairPage.pageSize,
    });
    pairTable.value = res.data.records;
    pairPage.total = res.data.total;
  } catch (error) {
    notifyApiError(error, "加载配对失败");
  }
};

const loadTaskLogs = async () => {
  if (!currentTaskId.value) {
    taskLogs.value = [];
    return;
  }
  try {
    const res = await plagiarismTaskLogsApi(currentTaskId.value);
    taskLogs.value = res.data;
  } catch (error) {
    notifyApiError(error, "加载日志失败");
  }
};

const loadTaskReport = async () => {
  if (!currentTaskId.value) {
    taskReport.value = null;
    return;
  }
  try {
    const res = await plagiarismTaskReportApi(currentTaskId.value);
    taskReport.value = res.data;
  } catch (error) {
    notifyApiError(error, "加载报告失败");
  }
};

const loadTrend = async () => {
  if (!form.assignmentId) {
    trendRows.value = [];
    return;
  }
  try {
    const params: { startAt?: string; endAt?: string; limit?: number } = {
      limit: trendFilter.limit,
    };
    if (trendFilter.range.length === 2) {
      params.startAt = trendFilter.range[0];
      params.endAt = trendFilter.range[1];
    }
    const res = await plagiarismAssignmentTrendApi(form.assignmentId, params);
    trendRows.value = res.data;
  } catch (error) {
    notifyApiError(error, "加载趋势失败");
  }
};

const selectTask = async (row: TaskRow) => {
  currentTaskId.value = row.id;
  currentTaskStatus.value = row.status;
  resetPairState();
  if (row.status === 0 || row.status === 1) {
    startPolling();
  } else {
    stopPolling();
  }
  await loadPairs();
  await loadTaskLogs();
  await loadTaskReport();
};

const cancelCurrentTask = async () => {
  if (!currentTaskId.value) return;
  try {
    await cancelPlagiarismTaskApi(currentTaskId.value);
    ElMessage.success("任务已取消");
    stopPolling();
    await loadTasks();
    await loadPairs();
    await loadTaskLogs();
    await loadTaskReport();
  } catch (error) {
    notifyApiError(error, "取消失败");
  }
};

const retryCurrentTask = async () => {
  if (!currentTaskId.value) return;
  try {
    const res = await retryPlagiarismTaskApi(currentTaskId.value);
    currentTaskId.value = res.data.id;
    currentTaskStatus.value = res.data.status;
    resetPairState();
    ElMessage.success("重试任务已创建");
    await loadTasks();
    await loadTaskLogs();
    await loadTaskReport();
    startPolling();
  } catch (error) {
    notifyApiError(error, "重试失败");
  }
};

const applyFilters = async () => {
  resetPairState();
  await loadPairs();
};

const applyTrendFilter = async () => {
  await loadTrend();
};

const resetTrendFilter = async () => {
  trendFilter.range = [];
  trendFilter.limit = 20;
  await loadTrend();
};

const exportCsv = async () => {
  if (!currentTaskId.value) return;
  try {
    const res = await exportPlagiarismPairsCsvApi(currentTaskId.value, {
      riskLevel: filters.riskLevel,
      minSimilarity: filters.minSimilarity,
    });
    downloadCsv(res.data, `plagiarism_task_${currentTaskId.value}.csv`);
  } catch (error) {
    notifyApiError(error, "导出失败");
  }
};

const exportAssignmentReport = async () => {
  if (!form.assignmentId) return;
  try {
    const res = await exportAssignmentPlagiarismReportApi(form.assignmentId);
    downloadCsv(res.data, `assignment_${form.assignmentId}_plagiarism_report.csv`);
  } catch (error) {
    notifyApiError(error, "导出报告失败");
  }
};

const buildTrendMeta = () => {
  return {
    generatedAt: new Date().toISOString(),
    startAt: trendFilter.range.length === 2 ? trendFilter.range[0] : "ALL",
    endAt: trendFilter.range.length === 2 ? trendFilter.range[1] : "ALL",
    limit: trendFilter.limit,
    points: trendRows.value.length,
  };
};

const buildTrendCanvas = (width = 1200, height = 640) => {
  if (trendRows.value.length === 0) {
    return null;
  }
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    return null;
  }

  const meta = buildTrendMeta();
  const left = 96;
  const right = 40;
  const top = 124;
  const bottom = 90;
  const chartWidth = canvas.width - left - right;
  const chartHeight = canvas.height - top - bottom;
  const rates = trendRows.value.map((row) => trendRiskPercent(Number(row.highRiskRate || 0)));
  const maxRate = Math.max(10, Math.ceil(Math.max(...rates) / 10) * 10);

  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  ctx.fillStyle = "#303133";
  ctx.font = "600 28px sans-serif";
  ctx.fillText(`作业 ${form.assignmentId} 查重趋势`, left, 40);
  ctx.font = "16px sans-serif";
  ctx.fillStyle = "#606266";
  ctx.fillText(`范围: ${meta.startAt} ~ ${meta.endAt}`, left, 68);
  ctx.fillText(`限制: ${meta.limit} | 数据点: ${meta.points} | 生成时间: ${meta.generatedAt}`, left, 92);

  ctx.strokeStyle = "#c0c4cc";
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(left, top);
  ctx.lineTo(left, top + chartHeight);
  ctx.lineTo(left + chartWidth, top + chartHeight);
  ctx.stroke();

  ctx.font = "16px sans-serif";
  ctx.fillStyle = "#909399";
  for (let i = 0; i <= 4; i++) {
    const y = top + (chartHeight * i) / 4;
    const value = Number((maxRate - (maxRate * i) / 4).toFixed(0));
    ctx.strokeStyle = "#ebeef5";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(left, y);
    ctx.lineTo(left + chartWidth, y);
    ctx.stroke();
    ctx.fillText(`${value}%`, left - 58, y + 6);
  }

  const step = Math.max(1, Math.ceil(trendRows.value.length / 8));
  const points = trendRows.value.map((row, idx) => {
    const rate = trendRiskPercent(Number(row.highRiskRate || 0));
    const x =
      trendRows.value.length === 1 ? left + chartWidth / 2 : left + (idx * chartWidth) / (trendRows.value.length - 1);
    const y = top + ((maxRate - rate) / maxRate) * chartHeight;
    return { x, y, rate, label: `T${row.taskId}` };
  });

  ctx.strokeStyle = "#409eff";
  ctx.lineWidth = 4;
  if (points.length > 1) {
    ctx.beginPath();
    points.forEach((point, idx) => {
      if (idx === 0) {
        ctx.moveTo(point.x, point.y);
      } else {
        ctx.lineTo(point.x, point.y);
      }
    });
    ctx.stroke();
  }

  points.forEach((point, idx) => {
    ctx.fillStyle = "#409eff";
    ctx.beginPath();
    ctx.arc(point.x, point.y, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = "#ffffff";
    ctx.beginPath();
    ctx.arc(point.x, point.y, 2.5, 0, Math.PI * 2);
    ctx.fill();

    if (idx % step === 0 || idx === points.length - 1) {
      ctx.fillStyle = "#606266";
      ctx.font = "14px sans-serif";
      ctx.fillText(point.label, point.x - 14, top + chartHeight + 26);
      ctx.fillText(`${point.rate}%`, point.x - 16, point.y - 12);
    }
  });

  return canvas;
};

const exportTrendCsv = () => {
  if (trendRows.value.length === 0) {
    ElMessage.warning("暂无趋势数据可导出");
    return;
  }
  const meta = buildTrendMeta();

  const lines = [
    "metaField,metaValue",
    `assignmentId,${form.assignmentId}`,
    `startAt,${meta.startAt}`,
    `endAt,${meta.endAt}`,
    `limit,${meta.limit}`,
    `points,${meta.points}`,
    `generatedAt,${meta.generatedAt}`,
    "",
    "taskId,status,threshold,totalPairs,highRiskPairs,highRiskRatePercent,createdAt,finishedAt",
    ...trendRows.value.map((row) =>
      [
        row.taskId,
        statusText(row.status),
        row.threshold ?? 0,
        row.totalPairs ?? 0,
        row.highRiskPairs ?? 0,
        trendRiskPercent(Number(row.highRiskRate || 0)),
        row.createdAt ?? "",
        row.finishedAt ?? "",
      ].join(",")
    ),
  ];
  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8;" });
  downloadBlob(blob, `assignment_${form.assignmentId}_trend.csv`);
};

const exportTrendPng = () => {
  const canvas = buildTrendCanvas();
  if (!canvas) {
    ElMessage.warning("暂无趋势数据可导出");
    return;
  }
  canvas.toBlob((blob) => {
    if (!blob) {
      ElMessage.error("导出PNG失败");
      return;
    }
    downloadBlob(blob, `assignment_${form.assignmentId}_trend.png`);
  }, "image/png");
};

const exportDefensePdf = async () => {
  const canvas = buildTrendCanvas(1400, 760);
  if (!canvas) {
    ElMessage.warning("暂无趋势数据可导出");
    return;
  }

  const { jsPDF } = await import("jspdf");
  const meta = buildTrendMeta();
  const pdf = new jsPDF({
    orientation: "p",
    unit: "mm",
    format: "a4",
    compress: true,
  });

  let y = 14;
  const ensureSpace = (needed = 6) => {
    if (y + needed > 287) {
      pdf.addPage();
      y = 14;
    }
  };
  const line = (text: string, size = 10, bold = false) => {
    ensureSpace(size >= 12 ? 8 : 6);
    pdf.setFont("helvetica", bold ? "bold" : "normal");
    pdf.setFontSize(size);
    pdf.text(text, 14, y);
    y += size >= 12 ? 7 : 5.5;
  };

  line(`作业 ${form.assignmentId} 查重分析报告`, 16, true);
  line(`生成时间: ${meta.generatedAt}`);
  line(`范围: ${meta.startAt} ~ ${meta.endAt}`);
  line(`限制: ${meta.limit}, 数据点: ${meta.points}`);
  line(`当前任务: ${currentTaskId.value ?? "-"}`);
  y += 2;

  line("报告快照", 12, true);
  if (!taskReport.value) {
    line("暂无任务快照。");
  } else {
    line(`任务ID: ${taskReport.value.taskId}, 状态: ${statusText(taskReport.value.status)}`);
    line(`阈值: ${taskReport.value.threshold}, 总配对数: ${taskReport.value.totalPairs ?? 0}`);
    line(`高风险配对数: ${taskReport.value.highRiskPairs ?? 0}`);
    line(
      `风险分布 => 低:${taskReport.value.lowRiskCount ?? 0}, 中:${taskReport.value.mediumRiskCount ?? 0}, 高:${taskReport.value.highRiskCount ?? 0}`
    );
  }

  y += 2;
  line("趋势图表", 12, true);
  const imgWidth = 182;
  const imgHeight = (canvas.height / canvas.width) * imgWidth;
  ensureSpace(imgHeight + 4);
  pdf.addImage(canvas.toDataURL("image/png"), "PNG", 14, y, imgWidth, imgHeight, undefined, "FAST");
  y += imgHeight + 4;

  if (taskReport.value?.topPairs?.length) {
    line("高风险配对 (前8)", 12, true);
    const top = taskReport.value.topPairs.slice(0, 8);
    top.forEach((pair, idx) => {
      line(
        `${idx + 1}. 配对#${pair.id} A:${pair.submissionAId} B:${pair.submissionBId} 相似度:${pair.similarity} 风险:${riskText(pair.riskLevel)} 汉明:${pair.hammingDistance}`
      );
    });
  }

  pdf.save(`assignment_${form.assignmentId}_defense_report.pdf`);
};

const handlePageChange = async (page: number) => {
  pairPage.pageNo = page;
  await loadPairs();
};

const handleSizeChange = async (size: number) => {
  pairPage.pageSize = size;
  pairPage.pageNo = 1;
  await loadPairs();
};

const openPairDetail = async (row: PairRow) => {
  try {
    const res = await plagiarismPairDetailApi(row.id);
    pairDetail.value = res.data;
    detailVisible.value = true;
  } catch (error) {
    notifyApiError(error, "加载详情失败");
  }
};

watch(() => route.query.assignmentId, async () => {
  await syncAssignmentContext();
}, { immediate: true });

onBeforeUnmount(() => {
  stopPolling();
});
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
}

.status-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-title {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.trend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.trend-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.trend-chart-wrap {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 8px;
  background: #fff;
}

.trend-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.trend-chart {
  width: 100%;
  height: 220px;
  display: block;
}

.risk-line {
  margin-bottom: 10px;
}

.frag-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
