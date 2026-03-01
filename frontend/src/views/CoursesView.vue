<template>
  <AppShell title="课程管理">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>课程列表</span>
          <el-button type="primary" @click="openCreate">新建课程</el-button>
        </div>
      </template>

      <el-table :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="courseCode" label="课程编码" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="teacherId" label="教师ID" width="100" />
        <el-table-column prop="semester" label="学期" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" title="新建课程" width="520px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="课程编码">
          <el-input v-model="form.courseCode" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.courseName" />
        </el-form-item>
        <el-form-item label="教师ID">
          <el-input-number v-model="form.teacherId" :min="1" :disabled="!isAdmin" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如：2025-2026-2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AppShell from "../components/AppShell.vue";
import { createCourseApi, listCoursesApi } from "../api/modules";
import { useAuthStore } from "../stores/auth";
import { notifyApiError } from "../utils/notify";

type Course = {
  id: number;
  courseCode: string;
  courseName: string;
  teacherId: number;
  semester: string;
  status: number;
};

const tableData = ref<Course[]>([]);
const showDialog = ref(false);
const authStore = useAuthStore();
const isAdmin = computed(() => authStore.user?.role === "ADMIN");
const form = reactive({
  courseCode: "",
  courseName: "",
  teacherId: 1,
  semester: "",
});

const loadData = async () => {
  const res = await listCoursesApi();
  tableData.value = res.data;
};

const openCreate = () => {
  if (!isAdmin.value && authStore.user?.id) {
    form.teacherId = authStore.user.id;
  }
  showDialog.value = true;
};

const submitCreate = async () => {
  if (!form.courseCode || !form.courseName || !form.semester) {
    ElMessage.warning("请填写完整课程信息");
    return;
  }
  try {
    await createCourseApi({
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
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
