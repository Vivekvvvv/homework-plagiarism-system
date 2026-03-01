<template>
  <el-tag :type="type" :size="size">
    {{ label }}
  </el-tag>
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
