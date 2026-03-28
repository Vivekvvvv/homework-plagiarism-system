<template>
  <AppShell title="作业管理">
    <div class="edu-page">
      <div class="edu-toolbar">
        <div>
          <div class="edu-toolbar__title">作业列表</div>
          <div class="edu-toolbar__sub">按课程筛选并管理作业任务</div>
        </div>
        <el-button type="primary" class="edu-btn-primary" @click="openCreate">+ 新建作业</el-button>
      </div>

      <div class="edu-filter-bar">
        <span class="edu-filter-label">课程 ID</span>
        <el-input-number v-model="courseId" :min="1" size="small" controls-position="right" />
        <el-button size="small" type="primary" class="edu-btn-primary" @click="loadData">加载</el-button>
        <span class="edu-badge">共 {{ tableData.length }} 项</span>
        <span v-if="focusedAssignmentId" class="edu-badge edu-badge--accent">已定位作业 #{{ focusedAssignmentId }}</span>
        <el-button v-if="routeFromDashboard" link type="primary" @click="markAssignmentCheckedAndBack">← 返回工作台</el-button>
      </div>

      <div class="edu-card">
        <el-table
          v-if="tableData.length > 0"
          :data="tableData"
          border
          stripe
          :row-class-name="tableRowClassName"
          class="edu-table"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="courseId" label="课程ID" width="100" />
          <el-table-column prop="deadline" label="截止时间" width="190" />
          <el-table-column prop="maxScore" label="满分" width="100" />
        </el-table>
        <el-empty v-else description="暂无作业数据，可点击右上角新建作业" />
      </div>
    </div>

    <el-dialog v-model="showDialog" title="新建作业" width="560px" class="edu-dialog">
      <el-form :model="form" label-width="92px" class="edu-form">
        <el-form-item label="课程ID">
          <el-input-number v-model="form.courseId" :min="1" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间"
          />
        </el-form-item>
        <el-form-item label="满分">
          <el-input-number v-model="form.maxScore" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="创建人ID">
          <el-input-number v-model="form.createdBy" :min="1" :disabled="true" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" class="edu-btn-primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import { createAssignmentApi, listAssignmentsApi } from "../api/modules";
import { readOptionalPositiveIntQuery, readPositiveIntQuery } from "../router/query";
import { buildDashboardReturnQuery, isFromDashboard } from "../router/dashboard-context";
import { useAuthStore } from "../stores/auth";
import { notifyApiError } from "../utils/notify";

type Assignment = {
  id: number;
  courseId: number;
  title: string;
  deadline: string;
  maxScore: number;
};

const courseId = ref(1);
const tableData = ref<Assignment[]>([]);
const showDialog = ref(false);
const focusedAssignmentId = ref<number | null>(null);
const routeFromDashboard = ref(false);
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const form = reactive({
  courseId: 1,
  title: "",
  description: "",
  deadline: "",
  maxScore: 100,
  createdBy: 1,
});

const reorderTableData = () => {
  if (!focusedAssignmentId.value) {
    return;
  }
  tableData.value = [...tableData.value].sort((left, right) => {
    if (left.id === focusedAssignmentId.value) return -1;
    if (right.id === focusedAssignmentId.value) return 1;
    return left.id - right.id;
  });
};

const loadData = async () => {
  try {
    const res = await listAssignmentsApi(courseId.value);
    tableData.value = Array.isArray(res.data) ? res.data : [];
    reorderTableData();
  } catch (error) {
    notifyApiError(error, "加载作业失败");
  }
};

const openCreate = () => {
  form.createdBy = authStore.user?.id || 1;
  showDialog.value = true;
};

const submitCreate = async () => {
  try {
    await createAssignmentApi({
      courseId: form.courseId,
      title: form.title,
      description: form.description,
      deadline: form.deadline,
      maxScore: form.maxScore,
      createdBy: form.createdBy,
    });
    ElMessage.success("创建成功");
    showDialog.value = false;
    await loadData();
  } catch (error) {
    notifyApiError(error, "创建失败");
  }
};

const tableRowClassName = ({ row }: { row: Assignment }) => {
  if (row.id === focusedAssignmentId.value) return "is-focus-row";
  return "";
};

const backToDashboard = (options?: { handled?: string; handledCount?: number }) => {
  router.push({
    path: "/dashboard",
    query: buildDashboardReturnQuery({
      focusAssignmentId: focusedAssignmentId.value,
      focusCourseId: courseId.value,
      handled: options?.handled,
      handledCount: options?.handledCount,
    }),
  });
};

const markAssignmentCheckedAndBack = () => {
  backToDashboard({
    handled: "assignment_checked",
    handledCount: 1,
  });
};

const applyRouteQuery = async () => {
  const nextCourseId = readPositiveIntQuery(route.query.courseId, courseId.value);
  const nextAssignmentId = readOptionalPositiveIntQuery(route.query.assignmentId);
  routeFromDashboard.value = isFromDashboard(route.query);
  const shouldReload = nextCourseId !== courseId.value || tableData.value.length === 0;

  courseId.value = nextCourseId;
  focusedAssignmentId.value = nextAssignmentId;

  if (shouldReload) {
    await loadData();
    return;
  }
  reorderTableData();
};

watch([() => route.query.courseId, () => route.query.assignmentId], async () => {
  await applyRouteQuery();
}, { immediate: true });
</script>

<style scoped>
.edu-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edu-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border: 1px solid #e2ede9;
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 2px 10px rgba(22, 163, 127, 0.06);
}

.edu-toolbar__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a2e26;
}

.edu-toolbar__sub {
  font-size: 12px;
  color: #6b8f82;
  margin-top: 3px;
}

.edu-btn-primary {
  background: linear-gradient(90deg, #16a37f, #0891b2) !important;
  border: none !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
}

.edu-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: #f8fdfb;
  border: 1px solid #e2ede9;
  border-radius: 12px;
  padding: 12px 18px;
}

.edu-filter-label {
  font-size: 12px;
  color: #6b8f82;
  font-weight: 500;
}

.edu-badge {
  font-size: 12px;
  background: #e6f7f3;
  color: #16a37f;
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 600;
}

.edu-badge--accent {
  background: #eff6ff;
  color: #2563eb;
}

.edu-card {
  background: #ffffff;
  border: 1px solid #e2ede9;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(22, 163, 127, 0.06);
}

.edu-table :deep(th) {
  background: #f0fdf8 !important;
  color: #2d5748 !important;
  font-weight: 700 !important;
}

.edu-table :deep(.is-focus-row) {
  --el-table-tr-bg-color: #e0f2fe;
}

.edu-dialog :deep(.el-dialog) {
  border-radius: 16px !important;
}

.edu-form :deep(.el-input__wrapper),
.edu-form :deep(.el-textarea__inner) {
  border-radius: 10px !important;
}
</style>
