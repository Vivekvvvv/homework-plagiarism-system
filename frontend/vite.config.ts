import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) {
            return undefined;
          }
          if (
            id.includes("/jspdf/") ||
            id.includes("/html2canvas/") ||
            id.includes("/canvg/") ||
            id.includes("/fflate/") ||
            id.includes("/dompurify/") ||
            id.includes("/svg-pathdata/")
          ) {
            return "vendor-pdf";
          }
          if (id.includes("@element-plus/icons-vue")) {
            return "vendor-element-icons";
          }
          if (id.includes("element-plus")) {
            return "vendor-element-plus";
          }
          if (id.includes("vue-router") || id.includes("pinia")) {
            return "vendor-router-state";
          }
          if (id.includes("axios")) {
            return "vendor-axios";
          }
          if (id.includes("/@vue/") || id.includes("/vue/")) {
            return "vendor-vue-core";
          }
          return "vendor-misc";
        },
      },
    },
  },
  server: {
    port: 5173,
  },
});
