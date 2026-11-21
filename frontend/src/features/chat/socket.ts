import { Client } from "@stomp/stompjs";
import { getAccessToken } from "../../services/apiClient";

function websocketUrl(): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");
  if (base.startsWith("/")) {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${protocol}//${window.location.host}${base.replace(/\/api\/v1$/, "")}/ws`;
  }
  return `${base.replace(/\/api\/v1$/, "").replace(/^http/, "ws")}/ws`;
}

export function createChatClient(): Client {
  const client = new Client({
    brokerURL: websocketUrl(),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    beforeConnect: () => {
      const token = getAccessToken();
      client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
    },
  });
  return client;
}
