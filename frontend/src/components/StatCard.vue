<template>
<el-card class="stat-card">
    <div class="stat-card__title">{{ title }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div v-if="hint" class="stat-card__hint">{{ hint }}</div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  value: string | number;
  hint?: string;
}>();
</script>

<style scoped>
.stat-card {
  border: 1px solid var(--app-border);
  background:
    linear-gradient(180deg, rgba(37, 99, 235, 0.06) 0%, rgba(255, 255, 255, 0.9) 60%),
    #ffffff;
  box-shadow: var(--app-shadow);
}

.stat-card__title {
  color: #64748b;
  font-size: 14px;
}

.stat-card__value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.stat-card__hint {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
