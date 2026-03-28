<template>
  <div class="stat-card">
    <div class="stat-card__label">{{ title }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div v-if="hint" class="stat-card__hint">{{ hint }}</div>
  </div>
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
  background: #ffffff;
  border: 1px solid #e2ede9;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 2px 10px rgba(22, 163, 127, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
}

.stat-card:hover {
  box-shadow: 0 4px 18px rgba(22, 163, 127, 0.13);
  transform: translateY(-1px);
}

.stat-card__label {
  font-size: 12px;
  font-weight: 500;
  color: #6b8f82;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #1a2e26;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-card__hint {
  margin-top: 6px;
  font-size: 12px;
  color: #16a37f;
  font-weight: 500;
}
</style>
