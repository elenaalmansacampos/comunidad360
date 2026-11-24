import type { Problem, TokenResponse } from "../types/api";
import { demoEnabled, demoRequest } from "./demoApi";

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");

let accessToken: string | null = null;

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly messageKey: string;
  readonly traceId?: string;

  constructor(status: number, code: string, messageKey: string, detail?: string, traceId?: string) {
    super(detail ?? messageKey);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.messageKey = messageKey;
    this.traceId = traceId;
  }
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

async function request<T>(path: string, options: RequestOptions = {}, retry = true): Promise<T> {
  const { body: rawBody, ...rest } = options;
  if (demoEnabled) {
    return demoRequest<T>(path, rest.method ?? "GET");
  }
  const headers = new Headers(rest.headers);
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let body: BodyInit | undefined;
  if (rawBody !== undefined) {
    if (rawBody instanceof FormData) {
      body = rawBody;
    } else if (typeof rawBody === "string") {
      body = rawBody;
    } else {
      headers.set("Content-Type", "application/json");
      body = JSON.stringify(rawBody);
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers,
    body,
    credentials: "include",
  });

  if (response.status === 401 && retry && !path.startsWith("/auth/")) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return request<T>(path, options, false);
    }
  }

  if (!response.ok) {
    let problem: Problem | undefined;
    try {
      problem = (await response.json()) as Problem;
    } catch {
      problem = undefined;
    }
    throw new ApiError(
      response.status,
      problem?.code ?? "UNKNOWN_ERROR",
      problem?.messageKey ?? "errors.generic",
      problem?.detail,
      problem?.traceId,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function refreshAccessToken(): Promise<boolean> {
  if (demoEnabled) {
    accessToken = "demo-token";
    return true;
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      credentials: "include",
      signal: controller.signal,
    });
    if (!response.ok) {
      accessToken = null;
      return false;
    }
    const data = (await response.json()) as TokenResponse;
    accessToken = data.accessToken;
    return true;
  } catch {
    accessToken = null;
    return false;
  } finally {
    clearTimeout(timeout);
  }
}

export async function downloadFile(path: string, retry = true): Promise<Blob> {
  if (demoEnabled) {
    return new Blob(["Comunidad 360 demo"], { type: "application/pdf" });
  }
  const headers = new Headers();
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  const response = await fetch(`${API_BASE}${path}`, {
    headers,
    credentials: "include",
  });
  if (response.status === 401 && retry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return downloadFile(path, false);
    }
  }
  if (!response.ok) {
    let problem: Problem | undefined;
    try {
      problem = (await response.json()) as Problem;
    } catch {
      problem = undefined;
    }
    throw new ApiError(
      response.status,
      problem?.code ?? "DOWNLOAD_ERROR",
      problem?.messageKey ?? "errors.generic",
      problem?.detail,
      problem?.traceId,
    );
  }
  return response.blob();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: "PATCH", body }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
