<template>
  <AppShell title="课程管理">
    <div class="edu-page">
      <div class="edu-toolbar">
        <div>
          <div class="edu-toolbar__title">课程列表</div>
          <div class="edu-toolbar__sub">管理所有开设课程及学生选课情况</div>
        </div>
        <el-button v-if="isAdmin || isTeacher" type="primary" class="edu-btn-primary" @click="openCreate">
          + 新建课程
        </el-button>
      </div>

      <div v-if="courses.length > 0" class="course-grid">
        <div v-for="c in courses" :key="c.id" class="course-card">
          <div class="course-card__header">
            <span class="course-card__code">{{ c.courseCode }}</span>
            <span class="course-card__semester">{{ c.semester }}</span>
          </div>
          <div class="course-card__name">{{ c.courseName }}</div>
          <div class="course-card__meta">课程 ID: {{ c.id }}</div>
        </div>
      </div>
      <el-empty v-else class="edu-empty" description="暂无课程，可点击右上角新建课程" />
    </div>

    <!-- 新建课程弹窗 -->
    <el-dialog v-model="showDialog" title="新建课程" width="480px" class="edu-dialog">
      <el-form :model="form" label-width="80px" class="edu-form">
        <el-form-item label="课程代码">
          <el-input v-model="form.courseCode" placeholder="如 CS101" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.courseName" placeholder="如 数据结构" />
        </el-form-item>
        <el-form-item v-if="isAdmin" label="教师ID">
          <el-input-number v-model="form.teacherId" :min="1" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如 2024-2025-1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" class="edu-btn-primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage } from "element-plus";
import AppShell from "../components/AppShell.vue";
import { listCourses, createCourse } from "../api/courses";
import { useAuthStore } from "../stores/auth";
import { notifyApiError } from "../utils/notify";

const authStore = useAuthStore();
const isAdmin = computed(() => authStore.user?.role === "admin");
const isTeacher = computed(() => authStore.user?.role === "teacher");

const courses = ref<any[]>([]);
const showDialog = ref(false);
const form = reactive({ courseCode: "", courseName: "", teacherId: 1, semester: "" });

const loadData = async () => {
  courses.value = await listCourses();
};

const openCreate = () => { showDialog.value = true; };

const handleCreate = async () => {
  try {
    await createCourse({
      courseCode: form.courseCode,
      courseName: form.courseName,
      teacherId: isAdmin.value ? form.teacherId : authStore.user?.id || form.teacherId,
      semester: form.semester,
    });
    ElMessage.success("创建成功");
    showDialog.value = false;
    form.courseCode = "";
    form.courseName = "";
    form.semester = "";
    await loadData();
  } catch (error) {
    notifyApiError(error, "创建失败");
  }
};

onMounted(async () => {
  try {
    await loadData();
  } catch (error) {
    notifyApiError(error, "加载课程失败");
  }
});
</script>

<style scoped>
.edu-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.course-card {
  background: #ffffff;
  border: 1px solid #e2ede9;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(22, 163, 127, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
}

.course-card:hover {
  box-shadow: 0 6px 20px rgba(22, 163, 127, 0.13);
  transform: translateY(-2px);
}

.course-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.course-card__code {
  background: #e6f7f3;
  color: #16a37f;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  letter-spacing: 0.3px;
}

.course-card__semester {
  font-size: 11px;
  color: #94a3b8;
}

.course-card__name {
  font-size: 15px;
  font-weight: 700;
  color: #1a2e26;
  margin-bottom: 8px;
}

.course-card__meta {
  font-size: 12px;
  color: #6b8f82;
}

.edu-empty {
  background: #ffffff;
  border: 1px solid #e2ede9;
  border-radius: 14px;
  padding: 40px;
}

.edu-dialog :deep(.el-dialog) {
  border-radius: 16px !important;
}

.edu-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
}
</style>
