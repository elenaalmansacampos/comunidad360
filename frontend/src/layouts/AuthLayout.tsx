import { Outlet } from "react-router-dom";
import { LanguageSwitcher } from "../components/LanguageSwitcher";

export function AuthLayout() {
  return (
    <div className="auth-shell">
      <header className="auth-top">
        <span className="brand">COMUNIDAD 360</span>
        <LanguageSwitcher />
      </header>
      <main className="auth-main">
        <Outlet />
      </main>
    </div>
  );
}
