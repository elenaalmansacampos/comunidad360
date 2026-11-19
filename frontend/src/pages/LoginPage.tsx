import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useNavigate } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { Input } from "../components/Input";
import { PasswordInput } from "../components/PasswordInput";
import { useAuthStore } from "../features/auth/authStore";

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const status = useAuthStore((state) => state.status);
  const login = useAuthStore((state) => state.login);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email, password);
      navigate("/", { replace: true });
    } catch {
      setError(t("auth.login.error"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="auth-card">
      <h1>{t("auth.login.title")}</h1>
      <p className="auth-card__subtitle">{t("auth.login.subtitle")}</p>

      {error ? <Alert variant="error">{error}</Alert> : null}

      <form className="form-stack" onSubmit={handleSubmit}>
        <Input
          label={t("common.email")}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
        <PasswordInput
          label={t("common.password")}
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        <Button type="submit" loading={busy}>
          {t("auth.login.submit")}
        </Button>
      </form>

      <p className="auth-card__note">{t("auth.login.noAccount")}</p>
    </section>
  );
}
