import { Navigate } from "react-router-dom";
import { FullPageLoader } from "../components/FullPageLoader";
import { useAuthStore } from "../features/auth/authStore";

export function HomeRedirect() {
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);

  if (status === "loading") {
    return <FullPageLoader />;
  }
  if (status === "anonymous") {
    return <Navigate to="/login" replace />;
  }
  if (user && user.memberships.length === 1) {
    return <Navigate to={`/communities/${user.memberships[0].id}/dashboard`} replace />;
  }
  return <Navigate to="/communities" replace />;
}
