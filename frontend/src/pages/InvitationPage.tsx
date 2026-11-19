import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Alert } from "../components/Alert";
import { Button } from "../components/Button";
import { Input } from "../components/Input";
import { PasswordInput } from "../components/PasswordInput";
import { useAuthStore } from "../features/auth/authStore";
import { api, ApiError } from "../services/apiClient";
import type { TokenResponse } from "../types/api";

export function InvitationPage() {
  const { t } = useTranslation();
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const establishSession = useAuthStore((state) => state.establishSession);
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!token) {
    return (
      <section className="auth-card">
        <h1>{t("auth.invitation.title")}</h1>
        <Alert variant="error">{t("auth.invitation.tokenMissing")}</Alert>
        <Link className="text-link" to="/login">
          {t("common.back")}
        </Link>
      </section>
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (password !== confirmPassword) {
      setError(t("auth.invitation.passwordMismatch"));
      return;
    }
    setBusy(true);
    try {
      const tokens = await api.post<TokenResponse>("/auth/invitations/accept", {
        token,
        fullName,
        password,
      });
      await establishSession(tokens.accessToken);
      navigate("/", { replace: true });
    } catch (caught) {
      if (caught instanceof ApiError && caught.status >= 400 && caught.status < 500) {
        setError(t("auth.invitation.tokenMissing"));
      } else {
        setError(t("common.unknownError"));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="auth-card">
      <h1>{t("auth.invitation.title")}</h1>
      <p className="auth-card__subtitle">{t("auth.invitation.subtitle")}</p>

      {error ? <Alert variant="error">{error}</Alert> : null}

      <form className="form-stack" onSubmit={handleSubmit}>
        <Input
          label={t("auth.invitation.fullName")}
          autoComplete="name"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          required
        />
        <PasswordInput
          label={t("auth.invitation.password")}
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={12}
          required
        />
        <PasswordInput
          label={t("auth.invitation.confirmPassword")}
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          minLength={12}
          required
        />
        <Button type="submit" loading={busy}>
          {t("auth.invitation.submit")}
        </Button>
      </form>

      <Link className="text-link" to="/login">
        {t("common.back")}
      </Link>
    </section>
  );
}
