import { Navigate, Outlet } from "react-router-dom";
import { FullPageLoader } from "../components/FullPageLoader";
import { useAuthStore } from "../features/auth/authStore";

export function ProtectedRoute() {
  const status = useAuthStore((state) => state.status);

  if (status === "loading") {
    return <FullPageLoader />;
  }
  if (status === "anonymous") {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}
