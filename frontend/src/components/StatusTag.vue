<template>
  <span class="status-tag" :class="[`status-tag--${type}`, `status-tag--${size}`]">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    type?: "primary" | "success" | "warning" | "danger" | "info";
    size?: "large" | "default" | "small";
  }>(),
  {
    type: "info",
    size: "small",
  }
);
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.3px;
  line-height: 1;
}

/* 尺寸 */
.status-tag--small  { font-size: 11px; padding: 3px 9px; }
.status-tag--default { font-size: 12px; padding: 4px 11px; }
.status-tag--large  { font-size: 13px; padding: 5px 14px; }

/* 语义色 */
.status-tag--info    { background: #e6f7f3; color: #16a37f; }
.status-tag--primary { background: #eff6ff; color: #2563eb; }
.status-tag--success { background: #f0fdf4; color: #16a34a; }
.status-tag--warning { background: #fffbeb; color: #d97706; }
.status-tag--danger  { background: #fff5f5; color: #ef4444; }
</style>
