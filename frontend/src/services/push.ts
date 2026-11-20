import { api } from "./apiClient";

export type PushState = "unsupported" | "unavailable" | "denied" | "subscribed" | "unsubscribed";

interface PushPublicKey {
  enabled: boolean;
  publicKey: string | null;
}

export function pushSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window
  );
}

export function urlBase64ToArrayBuffer(base64: string): ArrayBuffer {
  const padding = "=".repeat((4 - (base64.length % 4)) % 4);
  const normalized = (base64 + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = window.atob(normalized);
  const buffer = new ArrayBuffer(raw.length);
  const bytes = new Uint8Array(buffer);
  for (let index = 0; index < raw.length; index += 1) {
    bytes[index] = raw.charCodeAt(index);
  }
  return buffer;
}

async function registerWorker(): Promise<ServiceWorkerRegistration> {
  const base = import.meta.env.BASE_URL;
  return navigator.serviceWorker.register(`${base}sw.js`, { scope: base });
}

export async function getPushState(): Promise<PushState> {
  if (!pushSupported()) {
    return "unsupported";
  }
  try {
    const config = await api.get<PushPublicKey>("/me/push/public-key");
    if (!config.enabled || !config.publicKey) {
      return "unavailable";
    }
  } catch {
    return "unavailable";
  }
  if (Notification.permission === "denied") {
    return "denied";
  }
  const registration = await navigator.serviceWorker.getRegistration();
  const subscription = await registration?.pushManager.getSubscription();
  return subscription ? "subscribed" : "unsubscribed";
}

export async function enablePush(): Promise<void> {
  const config = await api.get<PushPublicKey>("/me/push/public-key");
  if (!config.enabled || !config.publicKey) {
    throw new Error("PUSH_UNAVAILABLE");
  }
  const permission = await Notification.requestPermission();
  if (permission !== "granted") {
    throw new Error("PUSH_DENIED");
  }
  const registration = await registerWorker();
  const existing = await registration.pushManager.getSubscription();
  const subscription =
    existing ??
    (await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToArrayBuffer(config.publicKey),
    }));
  const json = subscription.toJSON();
  await api.post("/me/push/subscriptions", {
    endpoint: json.endpoint,
    keys: {
      p256dh: json.keys?.p256dh,
      auth: json.keys?.auth,
    },
  });
}

export async function disablePush(): Promise<void> {
  const registration = await navigator.serviceWorker.getRegistration();
  const subscription = await registration?.pushManager.getSubscription();
  if (!subscription) {
    return;
  }
  try {
    await api.delete(`/me/push/subscriptions?endpoint=${encodeURIComponent(subscription.endpoint)}`);
  } finally {
    await subscription.unsubscribe();
  }
}
