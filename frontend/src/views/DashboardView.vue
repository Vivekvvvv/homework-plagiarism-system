<template>
  <AppShell title="系统概览">
    <div v-if="dashboardRole === 'teacher'" class="dashboard" v-loading="loading">
      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="24">
          <el-card class="dashboard-hero">
            <div class="quick-actions">
              <div>
                <div class="section-title">教师工作台</div>
                <div class="section-subtitle">聚合课程、作业、评阅与查重进度，优先暴露需要处理的事项。</div>
              </div>
              <div class="quick-actions__buttons">
                <el-button type="primary" @click="goTo('/assignments')">作业管理</el-button>
                <el-button @click="goTo('/reviews')">评阅中心</el-button>
                <el-button @click="goTo('/plagiarism')">查重任务</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="4" v-for="metric in teacherMetrics" :key="metric.title">
          <StatCard :title="metric.title" :value="metric.value" :hint="metric.hint" />
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="16">
          <el-card class="dashboard-hero">
            <template #header>
              <div class="panel-header">
                <span>重点作业</span>
                <el-tag type="info">共 {{ teacherSnapshot.focusAssignments.length }} 项</el-tag>
              </div>
            </template>
            <el-table
              v-if="teacherSnapshot.focusAssignments.length > 0"
              :data="teacherSnapshot.focusAssignments"
              border
              :row-class-name="focusAssignmentRowClassName"
            >
              <el-table-column prop="title" label="作业" min-width="180" />
              <el-table-column prop="courseName" label="课程" min-width="160" />
              <el-table-column prop="deadlineLabel" label="截止时间" width="168" />
              <el-table-column label="提交数" width="92">
                <template #default="{ row }">{{ row.submissionCount }}</template>
              </el-table-column>
              <el-table-column label="评阅率" width="102">
                <template #default="{ row }">{{ formatDashboardPercent(row.reviewedRate) }}%</template>
              </el-table-column>
              <el-table-column label="均分" width="90">
                <template #default="{ row }">{{ formatDashboardScore(row.averageScore) }}</template>
              </el-table-column>
              <el-table-column label="查重" width="120">
                <template #default="{ row }">
                  <el-tag :type="plagiarismTagType(row.plagiarismStatus, row.highRiskPairs)" size="small">
                    {{ row.highRiskPairs > 0 ? `高风险 ${row.highRiskPairs}` : row.plagiarismStatusText }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="提醒" min-width="180">
                <template #default="{ row }">
                  <el-tag :type="warningTagType(row.warningText)" size="small">{{ row.warningText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="处理" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToAssignments(row.courseId, row.assignmentId)">作业</el-button>
                  <el-button link type="primary" @click="goToReviewsByMode(row.assignmentId, row.pendingReviewCount > 0)">
                    {{ row.pendingReviewCount > 0 ? "连续评阅" : "评阅" }}
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    :loading="quickPlagiarismLoading[row.assignmentId] === true"
                    @click="handlePlagiarismAction(row)"
                  >
                    {{ canQuickStartPlagiarism(row) ? "一键查重" : "查重" }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="当前还没有可展示的作业数据" />
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>待处理事项</span>
                <el-tag :type="visibleTodos.length > 0 ? 'warning' : 'success'">
                  {{ visibleTodos.length > 0 ? `${visibleTodos.length} 项` : "已清空" }}
                </el-tag>
              </div>
            </template>
            <div v-if="visibleTodos.length > 0 || optimisticHandledLabel" class="alerts">
              <div v-if="optimisticHandledLabel" class="alert-item alert-item--dismissed">
                <div class="alert-item__title">刚处理完成</div>
                <div class="alert-item__meta">{{ optimisticHandledLabel }}</div>
              </div>
              <div
                v-for="todo in visibleTodos"
                :key="`${todo.assignmentId}-${todo.target}`"
                :class="['alert-item', { 'alert-item--focused': todo.assignmentId === focusedAssignmentId }]"
              >
                <div class="alert-item__header">
                  <div>
                    <div class="alert-item__title">{{ todo.title }}</div>
                    <div class="alert-item__meta">{{ todo.courseName }} · {{ todo.deadlineLabel }}</div>
                  </div>
                  <el-button link type="primary" @click="handleTodo(todo)">{{ todo.actionText }}</el-button>
                </div>
                <div class="todo-reasons">
                  <el-tag v-for="reason in todo.reasons" :key="`${todo.assignmentId}-${reason}`" :type="todo.urgency" size="small">
                    {{ reason }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无需要处理的待办事项" />
          </el-card>

          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>通知提醒</span>
                <div class="panel-actions">
                  <el-tag :type="unreadCount > 0 ? 'warning' : 'success'">
                    {{ unreadCount > 0 ? `${unreadCount} 未读` : "已读完" }}
                  </el-tag>
                  <el-button link type="primary" @click="markAllNotificationsRead">全部已读</el-button>
                </div>
              </div>
            </template>
            <div v-if="visibleNotifications.length > 0" class="alerts">
              <div v-for="notice in visibleNotifications" :key="`notice-${notice.id}`" class="alert-item">
                <div class="alert-item__header">
                  <div>
                    <div class="alert-item__title">{{ notice.title }}</div>
                    <div class="alert-item__meta">{{ notice.createdAt || "-" }}</div>
                  </div>
                  <el-button
                    v-if="notice.status === 0"
                    link
                    type="primary"
                    @click="markNotificationRead(notice.id)"
                  >
                    标记已读
                  </el-button>
                </div>
                <div class="todo-reasons">
                  <el-tag :type="notificationTagType(notice.level)" size="small">{{ notice.level || "info" }}</el-tag>
                  <el-tag :type="notice.status === 0 ? 'warning' : 'success'" size="small">
                    {{ notice.status === 0 ? "未读" : "已读" }}
                  </el-tag>
                </div>
                <div class="alert-item__meta">{{ notice.content || "无详细内容" }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无通知提醒" />
          </el-card>

          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>课程负载</span>
                <el-tag type="info">Top {{ teacherSnapshot.courseSummaries.length }}</el-tag>
              </div>
            </template>
            <div v-if="teacherSnapshot.courseSummaries.length > 0" class="course-list">
              <div v-for="course in teacherSnapshot.courseSummaries" :key="course.courseId" class="course-item">
                <div class="course-item__title">{{ course.courseName }}</div>
                <div class="course-item__meta">{{ course.courseCode }} · {{ course.semester }}</div>
                <div class="course-item__stats">
                  <span>作业 {{ course.assignmentCount }}</span>
                  <span>提交 {{ course.submissionCount }}</span>
                  <span>风险 {{ course.highRiskAssignments }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无课程负载数据" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div v-else class="dashboard" v-loading="loading">
      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="24">
          <el-card>
            <div class="quick-actions">
              <div>
                <div class="section-title">学生学习台</div>
                <div class="section-subtitle">聚合待提交作业、历史版本和教师反馈，优先帮你定位下一步动作。</div>
              </div>
              <div class="quick-actions__buttons">
                <el-button type="primary" @click="goToSubmissions()">提交中心</el-button>
                <el-button @click="loadDashboard">刷新概览</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="4" v-for="metric in studentMetrics" :key="metric.title">
          <StatCard :title="metric.title" :value="metric.value" :hint="metric.hint" />
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dashboard__row">
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>我的作业进度</span>
                <el-tag type="info">共 {{ studentMainAssignments.length }} 项</el-tag>
              </div>
            </template>
            <el-table
              v-if="studentMainAssignments.length > 0"
              :data="studentMainAssignments"
              border
              :row-class-name="focusAssignmentRowClassName"
            >
              <el-table-column prop="title" label="作业" min-width="170" />
              <el-table-column prop="courseName" label="课程" min-width="150" />
              <el-table-column prop="deadlineLabel" label="截止时间" width="168" />
              <el-table-column label="版本" width="92">
                <template #default="{ row }">
                  {{ row.latestVersionNo ? `v${row.latestVersionNo}` : "-" }}
                </template>
              </el-table-column>
              <el-table-column label="最近得分" width="98">
                <template #default="{ row }">{{ formatDashboardScore(row.latestScore) }}</template>
              </el-table-column>
              <el-table-column label="当前状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="studentStatusTagType(row.status)" size="small">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="提示" min-width="220">
                <template #default="{ row }">
                  <div>{{ row.feedbackText }}</div>
                  <div class="alert-item__meta">
                    {{ row.latestSubmitTimeLabel !== "-" ? `最近提交：${row.latestSubmitTimeLabel}` : `当前用户：${userName}` }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="处理" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goToSubmissions(row.assignmentId)">{{ row.actionText }}</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="当前没有可展示的作业进度" />
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>最新反馈</span>
                <el-tag :type="studentSnapshot.feedbackAssignments.length > 0 ? 'success' : 'info'">
                  {{ studentSnapshot.feedbackAssignments.length > 0 ? `${studentSnapshot.feedbackAssignments.length} 项` : "暂无" }}
                </el-tag>
              </div>
            </template>
            <div v-if="studentSnapshot.feedbackAssignments.length > 0" class="alerts">
              <div
                v-for="item in studentSnapshot.feedbackAssignments"
                :key="`feedback-${item.assignmentId}`"
                :class="['alert-item', { 'alert-item--focused': item.assignmentId === focusedAssignmentId }]"
              >
                <div class="alert-item__header">
                  <div>
                    <div class="alert-item__title">{{ item.title }}</div>
                    <div class="alert-item__meta">
                      {{ item.courseName }} · {{ item.latestReviewedAtLabel !== "-" ? item.latestReviewedAtLabel : item.deadlineLabel }}
                    </div>
                  </div>
                  <el-button link type="primary" @click="goToSubmissions(item.assignmentId)">查看</el-button>
                </div>
                <div class="todo-reasons">
                  <el-tag type="success" size="small">得分 {{ formatDashboardScore(item.latestScore) }}</el-tag>
                  <el-tag :type="studentStatusTagType(item.status)" size="small">{{ item.statusText }}</el-tag>
                </div>
                <div class="alert-item__meta">{{ item.feedbackText }}</div>
              </div>
            </div>
            <el-empty v-else description="还没有收到新的评阅反馈" />
          </el-card>

          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>我的待办</span>
                <el-tag :type="studentSnapshot.todoAssignments.length > 0 ? 'warning' : 'success'">
                  {{ studentSnapshot.todoAssignments.length > 0 ? `${studentSnapshot.todoAssignments.length} 项` : "已清空" }}
                </el-tag>
              </div>
            </template>
            <div v-if="studentSnapshot.todoAssignments.length > 0" class="alerts">
              <div
                v-for="item in studentSnapshot.todoAssignments"
                :key="`todo-${item.assignmentId}`"
                :class="['alert-item', { 'alert-item--focused': item.assignmentId === focusedAssignmentId }]"
              >
                <div class="alert-item__header">
                  <div>
                    <div class="alert-item__title">{{ item.title }}</div>
                    <div class="alert-item__meta">{{ item.courseName }} · {{ item.deadlineLabel }}</div>
                  </div>
                  <el-button link type="primary" @click="goToSubmissions(item.assignmentId)">{{ item.actionText }}</el-button>
                </div>
                <div class="todo-reasons">
                  <el-tag :type="studentStatusTagType(item.status)" size="small">{{ item.statusText }}</el-tag>
                  <el-tag v-if="item.latestVersionNo" type="info" size="small">最新版本 v{{ item.latestVersionNo }}</el-tag>
                </div>
                <div class="alert-item__meta">{{ item.feedbackText }}</div>
              </div>
            </div>
            <el-empty v-else description="当前没有需要处理的待办" />
          </el-card>

          <el-card class="dashboard__side-card">
            <template #header>
              <div class="panel-header">
                <span>通知提醒</span>
                <div class="panel-actions">
                  <el-tag :type="unreadCount > 0 ? 'warning' : 'success'">
                    {{ unreadCount > 0 ? `${unreadCount} 未读` : "已读完" }}
                  </el-tag>
                  <el-button link type="primary" @click="markAllNotificationsRead">全部已读</el-button>
                </div>
              </div>
            </template>
            <div v-if="visibleNotifications.length > 0" class="alerts">
              <div v-for="notice in visibleNotifications" :key="`notice-${notice.id}`" class="alert-item">
                <div class="alert-item__header">
                  <div>
                    <div class="alert-item__title">{{ notice.title }}</div>
                    <div class="alert-item__meta">{{ notice.createdAt || "-" }}</div>
                  </div>
                  <el-button
                    v-if="notice.status === 0"
                    link
                    type="primary"
                    @click="markNotificationRead(notice.id)"
                  >
                    标记已读
                  </el-button>
                </div>
                <div class="todo-reasons">
                  <el-tag :type="notificationTagType(notice.level)" size="small">{{ notice.level || "info" }}</el-tag>
                  <el-tag :type="notice.status === 0 ? 'warning' : 'success'" size="small">
                    {{ notice.status === 0 ? "未读" : "已读" }}
                  </el-tag>
                </div>
                <div class="alert-item__meta">{{ notice.content || "无详细内容" }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无通知提醒" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import AppShell from "../components/AppShell.vue";
import StatCard from "../components/StatCard.vue";
import { useAuthStore } from "../stores/auth";
import {
  createPlagiarismTaskApi,
  latestPlagiarismTaskApi,
  listAssignmentsApi,
  listCoursesApi,
  listNotificationsApi,
  listSubmissionReviewsApi,
  listSubmissionsApi,
  markNotificationsReadApi,
  submissionReviewSummaryApi,
} from "../api/modules";
import { readOptionalPositiveIntQuery, readPositiveIntQuery, readStringQuery } from "../router/query";
import { notifyApiError } from "../utils/notify";
import {
  buildStudentDashboardSnapshot,
  buildTeacherDashboardSnapshot,
  formatDashboardPercent,
  formatDashboardScore,
  resolveDashboardHandledMessage,
  resolveCoursesCount,
  resolveDashboardRole,
  resolveDashboardStatus,
  resolveDashboardUserName,
  type DashboardAssignment,
  type DashboardCourse,
  type DashboardPlagiarismTask,
  type DashboardReviewSummary,
  type DashboardSubmission,
  type DashboardSubmissionReview,
  type TeacherDashboardTodo,
} from "./dashboard.logic";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const coursesCount = ref(0);
const focusedAssignmentId = ref<number | null>(null);
const optimisticHandledAssignmentId = ref<number | null>(null);
const optimisticHandledLabel = ref("");
const optimisticHandledTimer = ref<ReturnType<typeof setTimeout> | null>(null);
const quickPlagiarismLoading = ref<Record<number, boolean>>({});
const userName = computed(() => resolveDashboardUserName(authStore.user));
const dashboardRole = computed(() => resolveDashboardRole(authStore.user?.role));
const systemStatus = resolveDashboardStatus();
const notifications = ref<any[]>([]);
const teacherSnapshot = ref(
  buildTeacherDashboardSnapshot({
    courses: [],
    assignments: [],
  })
);
const studentSnapshot = ref(
  buildStudentDashboardSnapshot({
    courses: [],
    assignments: [],
  })
);

const teacherMetrics = computed(() => [
  {
    title: "课程数",
    value: teacherSnapshot.value.courseCount,
    hint: "当前负责课程",
  },
  {
    title: "作业数",
    value: teacherSnapshot.value.assignmentCount,
    hint: "已聚合作业总量",
  },
  {
    title: "提交数",
    value: teacherSnapshot.value.submissionCount,
    hint: "所有作业提交数",
  },
  {
    title: "评阅覆盖",
    value: `${formatDashboardPercent(teacherSnapshot.value.reviewCoverageRate)}%`,
    hint: `${teacherSnapshot.value.reviewedSubmissionCount} 份已评阅`,
  },
  {
    title: "平均得分",
    value: formatDashboardScore(teacherSnapshot.value.averageScore),
    hint: "按已评阅提交加权",
  },
  {
    title: "高风险作业",
    value: teacherSnapshot.value.highRiskAssignments,
    hint: `${teacherSnapshot.value.overdueAssignments} 项已截止`,
  },
]);
const studentMetrics = computed(() => [
  {
    title: "课程数",
    value: studentSnapshot.value.courseCount,
    hint: "当前加入课程",
  },
  {
    title: "作业数",
    value: studentSnapshot.value.assignmentCount,
    hint: "本学期可见作业",
  },
  {
    title: "已提交",
    value: studentSnapshot.value.submittedAssignmentCount,
    hint: "已有提交记录",
  },
  {
    title: "待反馈",
    value: studentSnapshot.value.awaitingReviewCount,
    hint: "已提交但未收到评阅",
  },
  {
    title: "待提交",
    value: studentSnapshot.value.pendingAssignmentCount + studentSnapshot.value.overduePendingCount,
    hint: `${studentSnapshot.value.overduePendingCount} 项已截止`,
  },
  {
    title: "平均分",
    value: formatDashboardScore(studentSnapshot.value.latestAverageScore),
    hint: `${studentSnapshot.value.reviewedAssignmentCount} 项已有反馈`,
  },
]);
const studentMainAssignments = computed(() => {
  if (studentSnapshot.value.todoAssignments.length > 0) {
    return studentSnapshot.value.todoAssignments;
  }
  return studentSnapshot.value.recentAssignments;
});

const unreadCount = computed(() => notifications.value.filter((item) => item.status === 0).length);
const visibleNotifications = computed(() => {
  return [...notifications.value]
    .sort((a, b) => {
      const statusDiff = (a.status ?? 0) - (b.status ?? 0);
      if (statusDiff !== 0) return statusDiff;
      return (b.id || 0) - (a.id || 0);
    })
    .slice(0, 6);
});

const visibleTodos = computed(() => {
  if (!optimisticHandledAssignmentId.value) {
    return teacherSnapshot.value.todos;
  }
  return teacherSnapshot.value.todos.filter((todo) => todo.assignmentId !== optimisticHandledAssignmentId.value);
});

const clearOptimisticHandledState = () => {
  if (optimisticHandledTimer.value) {
    clearTimeout(optimisticHandledTimer.value);
    optimisticHandledTimer.value = null;
  }
  optimisticHandledAssignmentId.value = null;
  optimisticHandledLabel.value = "";
};

const startOptimisticHandledState = (assignmentId: number, label: string) => {
  clearOptimisticHandledState();
  optimisticHandledAssignmentId.value = assignmentId;
  optimisticHandledLabel.value = label;
  optimisticHandledTimer.value = setTimeout(() => {
    clearOptimisticHandledState();
  }, 1200);
};

const goTo = (path: string) => {
  router.push(path);
};

const goToAssignments = (courseId: number, assignmentId?: number) => {
  router.push({
    path: "/assignments",
    query: {
      from: "dashboard",
      courseId: String(courseId),
      ...(assignmentId ? { assignmentId: String(assignmentId), focusAssignmentId: String(assignmentId) } : {}),
      ...(courseId ? { focusCourseId: String(courseId) } : {}),
    },
  });
};

const goToReviews = (assignmentId: number) => {
  goToReviewsByMode(assignmentId, false);
};

const goToReviewsByMode = (assignmentId: number, pendingOnly: boolean) => {
  router.push({
    path: "/reviews",
    query: {
      from: "dashboard",
      assignmentId: String(assignmentId),
      focusAssignmentId: String(assignmentId),
      ...(pendingOnly ? { pendingOnly: "1" } : {}),
    },
  });
};

const goToPlagiarism = (assignmentId: number) => {
  router.push({
    path: "/plagiarism",
    query: {
      from: "dashboard",
      assignmentId: String(assignmentId),
      focusAssignmentId: String(assignmentId),
    },
  });
};

const goToSubmissions = (assignmentId?: number) => {
  router.push({
    path: "/submissions",
    query: {
      from: "dashboard",
      ...(assignmentId ? { assignmentId: String(assignmentId), focusAssignmentId: String(assignmentId) } : {}),
    },
  });
};

const handleTodo = (todo: TeacherDashboardTodo) => {
  if (todo.target === "plagiarism") {
    goToPlagiarism(todo.assignmentId);
    return;
  }
  if (todo.target === "reviews") {
    goToReviewsByMode(todo.assignmentId, true);
    return;
  }
  goToAssignments(todo.courseId, todo.assignmentId);
};

const canQuickStartPlagiarism = (row: {
  assignmentId: number;
  highRiskPairs: number;
  plagiarismStatus: number | null;
}) => {
  if (row.highRiskPairs > 0) {
    return false;
  }
  return row.plagiarismStatus === null || row.plagiarismStatus === 0 || row.plagiarismStatus === 3 || row.plagiarismStatus === 4;
};

const handlePlagiarismAction = async (row: {
  assignmentId: number;
  plagiarismStatus: number | null;
  highRiskPairs: number;
}) => {
  if (!canQuickStartPlagiarism(row)) {
    goToPlagiarism(row.assignmentId);
    return;
  }

  quickPlagiarismLoading.value = {
    ...quickPlagiarismLoading.value,
    [row.assignmentId]: true,
  };
  try {
    await createPlagiarismTaskApi({
      assignmentId: row.assignmentId,
      threshold: 0.7,
      simhashWeight: 0.7,
      jaccardWeight: 0.3,
      maxRetry: 1,
      runTimeoutSeconds: 120,
    });
    ElMessage.success(`作业 #${row.assignmentId} 的查重任务已发起`);
    await loadDashboard();
  } catch (error) {
    notifyApiError(error, "发起查重失败");
  } finally {
    quickPlagiarismLoading.value = {
      ...quickPlagiarismLoading.value,
      [row.assignmentId]: false,
    };
  }
};

const plagiarismTagType = (status: number | null, highRiskPairs: number) => {
  if (highRiskPairs > 0) return "danger";
  if (status === 2) return "success";
  if (status === 1) return "warning";
  if (status === 3) return "danger";
  return "info";
};

const warningTagType = (warningText: string) => {
  if (warningText.includes("高风险")) return "danger";
  if (warningText.includes("已截止")) return "warning";
  if (warningText.includes("临近")) return "info";
  return "success";
};

const notificationTagType = (level: string) => {
  if (level === "success") return "success";
  if (level === "warning") return "warning";
  if (level === "danger") return "danger";
  return "info";
};

const studentStatusTagType = (status: "pending_submission" | "awaiting_review" | "reviewed" | "overdue") => {
  if (status === "reviewed") return "success";
  if (status === "awaiting_review") return "warning";
  if (status === "overdue") return "danger";
  return "info";
};

const focusAssignmentRowClassName = ({ row }: { row: { assignmentId: number } }) => {
  return row.assignmentId === focusedAssignmentId.value ? "is-focus-row" : "";
};

const loadWithFallback = async <T,>(loader: () => Promise<T>, fallback: T, state: { partialErrors: number }) => {
  try {
    return await loader();
  } catch {
    state.partialErrors += 1;
    return fallback;
  }
};

const loadDashboard = async () => {
  loading.value = true;
  try {
    const state = { partialErrors: 0 };
    const coursesRes = await listCoursesApi();
    const courses = (Array.isArray(coursesRes.data) ? coursesRes.data : []) as DashboardCourse[];
    coursesCount.value = resolveCoursesCount(courses);

    const assignmentGroups = await Promise.all(
      courses.map(async (course) =>
        loadWithFallback(
          async () => {
            const res = await listAssignmentsApi(course.id);
            return (Array.isArray(res.data) ? res.data : []) as DashboardAssignment[];
          },
          [] as DashboardAssignment[],
          state
        )
      )
    );

    const assignments = assignmentGroups.flat();

    if (dashboardRole.value !== "teacher") {
      const submissionsByAssignment: Record<number, DashboardSubmission[]> = {};
      const reviewsByAssignment: Record<number, DashboardSubmissionReview[]> = {};

      await Promise.all(
        assignments.map(async (assignment) => {
          const [submissions, reviews] = await Promise.all([
            loadWithFallback(
              async () => {
                const res = await listSubmissionsApi(assignment.id);
                return (Array.isArray(res.data) ? res.data : []) as DashboardSubmission[];
              },
              [] as DashboardSubmission[],
              state
            ),
            loadWithFallback(
              async () => {
                const res = await listSubmissionReviewsApi(assignment.id);
                return (Array.isArray(res.data) ? res.data : []) as DashboardSubmissionReview[];
              },
              [] as DashboardSubmissionReview[],
              state
            ),
          ]);

          submissionsByAssignment[assignment.id] = submissions;
          reviewsByAssignment[assignment.id] = reviews;
        })
      );

      studentSnapshot.value = buildStudentDashboardSnapshot({
        courses,
        assignments,
        submissionsByAssignment,
        reviewsByAssignment,
        focusAssignmentId: focusedAssignmentId.value,
        now: new Date(),
      });

      if (state.partialErrors > 0) {
        ElMessage.warning(`部分学习看板数据加载失败，已展示可用结果（${state.partialErrors} 项）`);
      }
      return;
    }

    const submissionsByAssignment: Record<number, unknown[]> = {};
    const reviewSummaryByAssignment: Record<number, DashboardReviewSummary | null> = {};
    const plagiarismTaskByAssignment: Record<number, DashboardPlagiarismTask | null> = {};

    await Promise.all(
      assignments.map(async (assignment) => {
        const [submissions, reviewSummary, plagiarismTask] = await Promise.all([
          loadWithFallback(
            async () => {
              const res = await listSubmissionsApi(assignment.id);
              return Array.isArray(res.data) ? res.data : [];
            },
            [] as unknown[],
            state
          ),
          loadWithFallback(
            async () => {
              const res = await submissionReviewSummaryApi(assignment.id);
              return (res.data || null) as DashboardReviewSummary | null;
            },
            null,
            state
          ),
          loadWithFallback(
            async () => {
              const res = await latestPlagiarismTaskApi(assignment.id);
              return (res.data || null) as DashboardPlagiarismTask | null;
            },
            null,
            state
          ),
        ]);

        submissionsByAssignment[assignment.id] = submissions;
        reviewSummaryByAssignment[assignment.id] = reviewSummary;
        plagiarismTaskByAssignment[assignment.id] = plagiarismTask;
      })
    );

    teacherSnapshot.value = buildTeacherDashboardSnapshot({
      courses,
      assignments,
      submissionsByAssignment,
      reviewSummaryByAssignment,
      plagiarismTaskByAssignment,
      focusAssignmentId: focusedAssignmentId.value,
      now: new Date(),
    });

    if (state.partialErrors > 0) {
      ElMessage.warning(`部分看板数据加载失败，已展示可用结果（${state.partialErrors} 项）`);
    }
  } catch (error) {
    notifyApiError(error, "加载概览失败");
  } finally {
    loading.value = false;
  }
};

const loadNotifications = async () => {
  try {
    const res = await listNotificationsApi({ limit: 30 });
    notifications.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    notifyApiError(error, "加载通知失败");
  }
};

const markNotificationRead = async (id: number) => {
  try {
    await markNotificationsReadApi({ all: false, ids: [id] });
    await loadNotifications();
  } catch (error) {
    notifyApiError(error, "更新通知状态失败");
  }
};

const markAllNotificationsRead = async () => {
  if (notifications.value.length === 0) return;
  try {
    await markNotificationsReadApi({ all: true, ids: [] });
    await loadNotifications();
  } catch (error) {
    notifyApiError(error, "更新通知状态失败");
  }
};

watch(
  [() => route.query.focusAssignmentId, () => route.query.from, () => route.query.handled, () => route.query.handledCount],
  async () => {
    focusedAssignmentId.value = readOptionalPositiveIntQuery(route.query.focusAssignmentId);
    const from = readStringQuery(route.query.from);
    const handled = readStringQuery(route.query.handled);
    const handledCount = readPositiveIntQuery(route.query.handledCount, 1);
    const handledMessage = resolveDashboardHandledMessage(handled, handledCount);
    if (handledMessage) {
      if (focusedAssignmentId.value) {
        startOptimisticHandledState(focusedAssignmentId.value, handledMessage);
      }
      ElMessage.success(handledMessage);
    } else if (from === "dashboard" && focusedAssignmentId.value) {
      clearOptimisticHandledState();
      ElMessage.success(`已返回工作台，已定位作业 #${focusedAssignmentId.value}`);
    } else {
      clearOptimisticHandledState();
    }
    await loadDashboard();
    await loadNotifications();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  clearOptimisticHandledState();
});
</script>

<style scoped>
.dashboard__row + .dashboard__row {
  margin-top: 18px;
}

.dashboard-hero {
  border: 1px solid var(--app-border);
  background:
    radial-gradient(420px 200px at 90% -10%, rgba(37, 99, 235, 0.08), transparent 60%),
    #ffffff;
}

.quick-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.quick-actions__buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 19px;
  font-weight: 700;
  color: #101828;
}

.section-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--app-border);
}

:deep(.el-empty) {
  padding: 24px 0 16px;
  color: #94a3b8;
}

:deep(.el-table th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}


:deep(.el-table .is-focus-row) {
  --el-table-tr-bg-color: #ecf5ff;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dashboard__side-card + .dashboard__side-card {
  margin-top: 16px;
}

.alerts,
.course-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item,
.course-item {
  padding: 12px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #f8fafc;
}

.alert-item__title,
.course-item__title {
  font-size: 14px;
  font-weight: 600;
  color: #101828;
}

.alert-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.alert-item--focused {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.18);
}

.alert-item--dismissed {
  opacity: 0.55;
  background: #f2f4f7;
  border-style: dashed;
}

.alert-item__meta {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.todo-reasons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.course-item__meta {
  margin-top: 6px;
  color: #667085;
  font-size: 12px;
}

.course-item__stats {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: #344054;
  font-size: 12px;
}
</style>
