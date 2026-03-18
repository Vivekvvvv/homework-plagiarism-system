import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as Sentry from "@sentry/vue";

import App from "./App.vue";
import router from "./router";
import "./styles.css";
import { useAuthStore } from "./stores/auth";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);
app.use(ElementPlus);

const sentryDsn = import.meta.env.VITE_SENTRY_DSN;
if (sentryDsn) {
  Sentry.init({
    app,
    dsn: sentryDsn,
    integrations: [Sentry.browserTracingIntegration({ router })],
    tracesSampleRate: 1.0,
  });
}

const authStore = useAuthStore(pinia);
if (authStore.token) {
  authStore.fetchMe().catch(() => {
    authStore.logout();
  });
}

app.mount("#app");
