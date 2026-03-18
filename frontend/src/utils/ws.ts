import { ref } from "vue";
import { getStoredToken } from "../api/config";

export type WsNotification = {
  id?: number;
  userId?: number;
  title?: string;
  content?: string;
  level?: string;
  sourceType?: string;
  sourceId?: string;
};

const WS_RECONNECT_DELAY = 3000;
const WS_MAX_RETRIES = 10;

let ws: WebSocket | null = null;
let retryCount = 0;
let retryTimer: ReturnType<typeof setTimeout> | null = null;

export const wsConnected = ref(false);
export const latestNotification = ref<WsNotification | null>(null);
export const unreadBadge = ref(0);

function buildWsUrl(): string {
  const token = getStoredToken();
  if (!token) return "";
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = import.meta.env.PROD
    ? window.location.host
    : "localhost:8081";
  return `${protocol}//${host}/ws/notifications?token=${encodeURIComponent(token)}`;
}

export function connectWs(onMessage?: (notification: WsNotification) => void): void {
  closeWs();
  const url = buildWsUrl();
  if (!url) return;

  try {
    ws = new WebSocket(url);
  } catch {
    scheduleReconnect(onMessage);
    return;
  }

  ws.onopen = () => {
    wsConnected.value = true;
    retryCount = 0;
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data) as WsNotification;
      latestNotification.value = data;
      unreadBadge.value += 1;
      onMessage?.(data);
    } catch {
      // ignore malformed messages
    }
  };

  ws.onclose = () => {
    wsConnected.value = false;
    scheduleReconnect(onMessage);
  };

  ws.onerror = () => {
    ws?.close();
  };
}

export function closeWs(): void {
  if (retryTimer) {
    clearTimeout(retryTimer);
    retryTimer = null;
  }
  if (ws) {
    ws.onclose = null;
    ws.onerror = null;
    ws.close();
    ws = null;
  }
  wsConnected.value = false;
}

export function resetBadge(): void {
  unreadBadge.value = 0;
}

function scheduleReconnect(onMessage?: (notification: WsNotification) => void): void {
  if (retryCount >= WS_MAX_RETRIES) return;
  retryCount++;
  retryTimer = setTimeout(() => connectWs(onMessage), WS_RECONNECT_DELAY);
}
