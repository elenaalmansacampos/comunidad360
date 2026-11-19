import { create } from "zustand";
import i18n from "../../locales/i18n";
import { api, refreshAccessToken, setAccessToken } from "../../services/apiClient";
import type { Language, TokenResponse, UserProfile } from "../../types/api";

type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthState {
  user: UserProfile | null;
  status: AuthStatus;
  bootstrap: () => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  establishSession: (accessToken: string) => Promise<void>;
  logout: () => Promise<void>;
  setLanguage: (language: Language) => Promise<void>;
}

async function loadUser(): Promise<UserProfile> {
  return api.get<UserProfile>("/me");
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: "loading",

  bootstrap: async () => {
    set({ status: "loading" });
    const refreshed = await refreshAccessToken();
    if (!refreshed) {
      set({ user: null, status: "anonymous" });
      return;
    }
    try {
      const user = await loadUser();
      set({ user, status: "authenticated" });
      await i18n.changeLanguage(user.preferredLanguage);
    } catch {
      setAccessToken(null);
      set({ user: null, status: "anonymous" });
    }
  },

  login: async (email, password) => {
    const tokens = await api.post<TokenResponse>("/auth/login", { email, password });
    setAccessToken(tokens.accessToken);
    const user = await loadUser();
    set({ user, status: "authenticated" });
    await i18n.changeLanguage(user.preferredLanguage);
  },

  establishSession: async (accessToken) => {
    setAccessToken(accessToken);
    const user = await loadUser();
    set({ user, status: "authenticated" });
    await i18n.changeLanguage(user.preferredLanguage);
  },

  logout: async () => {
    try {
      await api.post<void>("/auth/logout");
    } finally {
      setAccessToken(null);
      set({ user: null, status: "anonymous" });
    }
  },

  setLanguage: async (language) => {
    const user = await api.put<UserProfile>("/me/language", { language });
    set({ user });
    await i18n.changeLanguage(language);
  },
}));
