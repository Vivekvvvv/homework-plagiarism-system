<template>
  <AppShell title="评估与审计">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="查重评估" name="evaluation">
        <el-row :gutter="16">
          <el-col :span="10">
            <el-card>
              <template #header>
                <span>新增评估样本</span>
              </template>
              <el-form :model="caseForm" label-width="110px">
                <el-form-item label="样本名称">
                  <el-input v-model="caseForm.caseName" />
                </el-form-item>
                <el-form-item label="文本 A">
                  <el-input v-model="caseForm.textA" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item label="文本 B">
                  <el-input v-model="caseForm.textB" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item label="期望风险">
                  <el-select v-model="caseForm.expectedRiskLevel" style="width: 100%">
                    <el-option :value="1" label="低风险" />
                    <el-option :value="2" label="中风险" />
                    <el-option :value="3" label="高风险" />
                  </el-select>
                </el-form-item>
                <el-form-item label="备注">
                  <el-input v-model="caseForm.note" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="creatingCase" @click="createCase">新增样本</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card style="margin-top: 16px">
              <template #header>
                <span>运行评估</span>
              </template>
              <el-form :model="runForm" label-width="110px">
                <el-form-item label="阈值">
                  <el-input-number v-model="runForm.threshold" :min="0.1" :max="1" :step="0.01" :precision="2" />
                </el-form-item>
                <el-form-item label="SimHash 权重">
                  <el-input-number v-model="runForm.simhashWeight" :min="0" :max="1" :step="0.05" :precision="2" />
                </el-form-item>
                <el-form-item label="Jaccard 权重">
                  <el-input-number v-model="runForm.jaccardWeight" :min="0" :max="1" :step="0.05" :precision="2" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="runningEval" @click="runEvaluation">执行评估</el-button>
                  <el-button @click="reloadEvaluation">刷新数据</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <el-col :span="14">
            <el-card>
              <template #header>
                <span>评估指标</span>
              </template>
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-statistic title="总样本" :value="report.totalCases || 0" />
                </el-col>
                <el-col :span="8">
                  <el-statistic title="已评估" :value="report.evaluatedCases || 0" />
                </el-col>
                <el-col :span="8">
                  <el-statistic title="准确率" :value="toPercentNumber(report.accuracy)" :formatter="formatPercent" />
                </el-col>
              </el-row>
              <el-row :gutter="12" style="margin-top: 12px">
                <el-col :span="8">
                  <el-statistic
                    title="宏平均 Precision"
                    :value="toPercentNumber(report.macroPrecision)"
                    :formatter="formatPercent"
                  />
                </el-col>
                <el-col :span="8">
                  <el-statistic
                    title="宏平均 Recall"
                    :value="toPercentNumber(report.macroRecall)"
                    :formatter="formatPercent"
                  />
                </el-col>
                <el-col :span="8">
                  <el-statistic
                    title="宏平均 F1"
                    :value="toPercentNumber(report.macroF1)"
                    :formatter="formatPercent"
                  />
                </el-col>
              </el-row>

              <el-divider>混淆矩阵</el-divider>
              <el-table :data="report.confusionMatrix || []" border size="small">
                <el-table-column prop="expectedRiskLevel" label="真实风险" width="100">
                  <template #default="{ row }">{{ riskText(row.expectedRiskLevel) }}</template>
                </el-table-column>
                <el-table-column prop="predictedLow" label="预测低" width="100" />
                <el-table-column prop="predictedMedium" label="预测中" width="100" />
                <el-table-column prop="predictedHigh" label="预测高" width="100" />
              </el-table>

              <el-divider>按风险等级指标</el-divider>
              <el-table :data="report.perRiskMetrics || []" border size="small">
                <el-table-column prop="riskLevel" label="风险等级" width="100">
                  <template #default="{ row }">{{ riskText(row.riskLevel) }}</template>
                </el-table-column>
                <el-table-column label="Precision">
                  <template #default="{ row }">{{ toPercent(row.precision) }}</template>
                </el-table-column>
                <el-table-column label="Recall">
                  <template #default="{ row }">{{ toPercent(row.recall) }}</template>
                </el-table-column>
                <el-table-column label="F1">
                  <template #default="{ row }">{{ toPercent(row.f1) }}</template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>样本列表</span>
          </template>
          <el-table :data="cases" border>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="caseName" label="样本名称" width="160" />
            <el-table-column prop="expectedRiskLevel" label="期望风险" width="90">
              <template #default="{ row }">{{ riskText(row.expectedRiskLevel) }}</template>
            </el-table-column>
            <el-table-column prop="predictedRiskLevel" label="预测风险" width="90">
              <template #default="{ row }">{{ row.predictedRiskLevel ? riskText(row.predictedRiskLevel) : "-" }}</template>
            </el-table-column>
            <el-table-column prop="simhashSimilarity" label="SimHash" width="100" />
            <el-table-column prop="jaccardSimilarity" label="Jaccard" width="100" />
            <el-table-column prop="fusedSimilarity" label="融合相似度" width="110" />
            <el-table-column prop="evaluatedAt" label="评估时间" width="180" />
            <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="评估趋势" name="trend">
        <el-card>
          <template #header>
            <div class="toolbar">
              <div class="toolbar-left">
                <span>趋势窗口</span>
                <el-input-number v-model="evalRunLimit" :min="5" :max="200" :step="5" />
                <el-button @click="loadEvalRuns">刷新趋势</el-button>
              </div>
            </div>
          </template>
          <div v-if="evalRuns.length > 0" class="trend-chart-wrap">
            <svg :viewBox="`0 0 ${evalChartWidth} ${evalChartHeight}`" class="trend-chart" role="img">
              <line
                :x1="evalChartPadding.left"
                :y1="evalChartPadding.top"
                :x2="evalChartPadding.left"
                :y2="evalChartHeight - evalChartPadding.bottom"
                stroke="#c0c4cc"
              />
              <line
                :x1="evalChartPadding.left"
                :y1="evalChartHeight - evalChartPadding.bottom"
                :x2="evalChartWidth - evalChartPadding.right"
                :y2="evalChartHeight - evalChartPadding.bottom"
                stroke="#c0c4cc"
              />

              <g v-for="tick in evalChartData.yTicks" :key="tick.value">
                <line
                  :x1="evalChartPadding.left"
                  :y1="tick.y"
                  :x2="evalChartWidth - evalChartPadding.right"
                  :y2="tick.y"
                  stroke="#ebeef5"
                />
                <text :x="evalChartPadding.left - 8" :y="tick.y + 4" text-anchor="end" fill="#909399" font-size="12">
                  {{ tick.value }}%
                </text>
              </g>

              <polyline
                v-if="evalChartData.accuracyPoints.length > 1"
                :points="evalChartData.accuracyPolyline"
                fill="none"
                stroke="#409eff"
                stroke-width="2.5"
              />
              <polyline
                v-if="evalChartData.recallPoints.length > 1"
                :points="evalChartData.recallPolyline"
                fill="none"
                stroke="#10b981"
                stroke-width="2.5"
              />
              <polyline
                v-if="evalChartData.f1Points.length > 1"
                :points="evalChartData.f1Polyline"
                fill="none"
                stroke="#f59e0b"
                stroke-width="2.5"
              />

              <g v-for="point in evalChartData.accuracyPoints" :key="`acc-${point.label}-${point.x}`">
                <circle :cx="point.x" :cy="point.y" r="4.5" fill="#409eff" />
                <circle :cx="point.x" :cy="point.y" r="2" fill="#ffffff" />
                <title>{{ point.label }} 准确率 {{ point.value }}%</title>
              </g>
              <g v-for="point in evalChartData.recallPoints" :key="`recall-${point.label}-${point.x}`">
                <circle :cx="point.x" :cy="point.y" r="4.5" fill="#10b981" />
                <circle :cx="point.x" :cy="point.y" r="2" fill="#ffffff" />
                <title>{{ point.label }} Recall {{ point.value }}%</title>
              </g>
              <g v-for="point in evalChartData.f1Points" :key="`f1-${point.label}-${point.x}`">
                <circle :cx="point.x" :cy="point.y" r="4.5" fill="#f59e0b" />
                <circle :cx="point.x" :cy="point.y" r="2" fill="#ffffff" />
                <title>{{ point.label }} F1 {{ point.value }}%</title>
              </g>

              <text
                v-for="point in evalLabelPoints"
                :key="`label-${point.label}-${point.x}`"
                :x="point.x"
                :y="evalChartHeight - evalChartPadding.bottom + 16"
                text-anchor="middle"
                fill="#909399"
                font-size="11"
              >
                {{ point.label }}
              </text>
            </svg>
            <div class="trend-legend">
              <span class="legend-item"><i class="legend-dot legend-dot--acc"></i>准确率</span>
              <span class="legend-item"><i class="legend-dot legend-dot--recall"></i>Recall</span>
              <span class="legend-item"><i class="legend-dot legend-dot--f1"></i>F1</span>
            </div>
          </div>
          <el-empty v-else description="暂无评估趋势数据" />

          <el-table :data="evalRuns" border size="small" style="margin-top: 12px">
            <el-table-column prop="id" label="Run" width="80" />
            <el-table-column prop="threshold" label="阈值" width="100" />
            <el-table-column prop="simhashWeight" label="SimHash权重" width="120" />
            <el-table-column prop="jaccardWeight" label="Jaccard权重" width="120" />
            <el-table-column label="准确率" width="110">
              <template #default="{ row }">{{ toPercent(row.accuracy) }}</template>
            </el-table-column>
            <el-table-column label="Recall" width="110">
              <template #default="{ row }">{{ toPercent(row.macroRecall) }}</template>
            </el-table-column>
            <el-table-column label="F1" width="110">
              <template #default="{ row }">{{ toPercent(row.macroF1) }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="运行时间" min-width="170" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="audit">
        <el-card>
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="auditFilter.actorUsername" placeholder="用户名筛选" style="width: 180px" clearable />
              <el-input v-model="auditFilter.action" placeholder="动作筛选" style="width: 220px" clearable />
              <el-input-number v-model="auditFilter.limit" :min="1" :max="500" />
              <el-button type="primary" @click="loadAuditLogs">查询</el-button>
            </div>
          </div>
          <el-table :data="auditLogs" border style="margin-top: 12px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="actorUsername" label="用户" width="110" />
            <el-table-column prop="actorRole" label="角色" width="100" />
            <el-table-column prop="action" label="动作" width="180" />
            <el-table-column prop="targetType" label="目标类型" width="140" />
            <el-table-column prop="targetId" label="目标ID" width="120" />
            <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
            <el-table-column prop="requestMethod" label="方法" width="90" />
            <el-table-column prop="requestPath" label="路径" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AppShell from "../components/AppShell.vue";
import {
  createPlagiarismEvalCaseApi,
  listAuditLogsApi,
  listPlagiarismEvalCasesApi,
  listPlagiarismEvalRunsApi,
  plagiarismEvaluationReportApi,
  runPlagiarismEvaluationApi,
} from "../api/modules";
import { buildEvalRunChartData, formatPercent, riskText, toPercent, toPercentNumber } from "./evaluation.logic";
import { notifyApiError } from "../utils/notify";

const activeTab = ref("evaluation");
const creatingCase = ref(false);
const runningEval = ref(false);
const cases = ref<any[]>([]);
const auditLogs = ref<any[]>([]);
const evalRuns = ref<any[]>([]);
const report = reactive<any>({
  totalCases: 0,
  evaluatedCases: 0,
  accuracy: 0,
  macroPrecision: 0,
  macroRecall: 0,
  macroF1: 0,
  perRiskMetrics: [],
  confusionMatrix: [],
});

const evalChartWidth = 760;
const evalChartHeight = 220;
const evalChartPadding = {
  left: 50,
  right: 20,
  top: 16,
  bottom: 30,
};

const caseForm = reactive({
  caseName: "",
  textA: "",
  textB: "",
  expectedRiskLevel: 2,
  note: "",
});

const runForm = reactive({
  threshold: 0.7,
  simhashWeight: 0.7,
  jaccardWeight: 0.3,
});

const auditFilter = reactive({
  actorUsername: "",
  action: "",
  limit: 100,
});

const evalRunLimit = ref(20);

const evalChartData = computed(() =>
  buildEvalRunChartData(evalRuns.value, evalChartWidth, evalChartHeight, evalChartPadding)
);

const evalLabelPoints = computed(() =>
  evalChartData.value.accuracyPoints.filter((point) => point.showLabel)
);


const loadCases = async () => {
  const res = await listPlagiarismEvalCasesApi(1);
  cases.value = res.data;
};

const loadReport = async () => {
  const res = await plagiarismEvaluationReportApi();
  Object.assign(report, res.data || {});
};

const reloadEvaluation = async () => {
  try {
    await Promise.all([loadCases(), loadReport()]);
  } catch (error) {
    notifyApiError(error, "加载评估数据失败");
  }
};

const createCase = async () => {
  if (!caseForm.caseName || !caseForm.textA || !caseForm.textB) {
    ElMessage.warning("请完整填写样本信息");
    return;
  }
  creatingCase.value = true;
  try {
    await createPlagiarismEvalCaseApi(caseForm);
    ElMessage.success("样本已创建");
    caseForm.caseName = "";
    caseForm.textA = "";
    caseForm.textB = "";
    caseForm.note = "";
    await reloadEvaluation();
  } catch (error) {
    notifyApiError(error, "创建样本失败");
  } finally {
    creatingCase.value = false;
  }
};

const runEvaluation = async () => {
  runningEval.value = true;
  try {
    await runPlagiarismEvaluationApi({
      threshold: runForm.threshold,
      simhashWeight: runForm.simhashWeight,
      jaccardWeight: runForm.jaccardWeight,
    });
    ElMessage.success("评估执行完成");
    await Promise.all([reloadEvaluation(), loadEvalRuns()]);
  } catch (error) {
    notifyApiError(error, "执行评估失败");
  } finally {
    runningEval.value = false;
  }
};

const loadEvalRuns = async () => {
  try {
    const res = await listPlagiarismEvalRunsApi(evalRunLimit.value);
    evalRuns.value = Array.isArray(res.data) ? res.data.reverse() : [];
  } catch (error) {
    notifyApiError(error, "加载评估趋势失败");
  }
};

const loadAuditLogs = async () => {
  try {
    const res = await listAuditLogsApi({
      actorUsername: auditFilter.actorUsername || undefined,
      action: auditFilter.action || undefined,
      limit: auditFilter.limit,
    });
    auditLogs.value = res.data;
  } catch (error) {
    notifyApiError(error, "加载审计日志失败");
  }
};

onMounted(async () => {
  await reloadEvaluation();
  await loadEvalRuns();
  await loadAuditLogs();
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trend-chart-wrap {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 8px;
  background: #fff;
}

.trend-chart {
  width: 100%;
  height: 220px;
  display: block;
}

.trend-legend {
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

.legend-dot--acc {
  background: #409eff;
}

.legend-dot--recall {
  background: #10b981;
}

.legend-dot--f1 {
  background: #f59e0b;
}
</style>
