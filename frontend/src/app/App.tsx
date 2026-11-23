import { lazy, Suspense, useEffect, useRef } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { FullPageLoader } from "../components/FullPageLoader";
import { useAuthStore } from "../features/auth/authStore";
import { AppLayout } from "../layouts/AppLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { HomeRedirect } from "../pages/HomeRedirect";
import { LoginPage } from "../pages/LoginPage";
import { ProtectedRoute } from "./ProtectedRoute";

const InvitationPage = lazy(() => import("../pages/InvitationPage").then((m) => ({ default: m.InvitationPage })));
const CommunitySelectPage = lazy(() =>
  import("../pages/CommunitySelectPage").then((m) => ({ default: m.CommunitySelectPage })));
const OrganizationsPage = lazy(() =>
  import("../pages/OrganizationsPage").then((m) => ({ default: m.OrganizationsPage })));
const OrganizationDetailPage = lazy(() =>
  import("../pages/OrganizationDetailPage").then((m) => ({ default: m.OrganizationDetailPage })));
const DashboardPage = lazy(() => import("../pages/DashboardPage").then((m) => ({ default: m.DashboardPage })));
const StructurePage = lazy(() => import("../pages/StructurePage").then((m) => ({ default: m.StructurePage })));
const UnitDetailPage = lazy(() => import("../pages/UnitDetailPage").then((m) => ({ default: m.UnitDetailPage })));
const FinancePage = lazy(() => import("../pages/FinancePage").then((m) => ({ default: m.FinancePage })));
const BankingPage = lazy(() => import("../pages/BankingPage").then((m) => ({ default: m.BankingPage })));
const IncidentsPage = lazy(() => import("../pages/IncidentsPage").then((m) => ({ default: m.IncidentsPage })));
const IncidentDetailPage = lazy(() =>
  import("../pages/IncidentDetailPage").then((m) => ({ default: m.IncidentDetailPage })));
const DocumentsPage = lazy(() => import("../pages/DocumentsPage").then((m) => ({ default: m.DocumentsPage })));
const DocumentDetailPage = lazy(() =>
  import("../pages/DocumentDetailPage").then((m) => ({ default: m.DocumentDetailPage })));
const MeetingsPage = lazy(() => import("../pages/MeetingsPage").then((m) => ({ default: m.MeetingsPage })));
const MeetingDetailPage = lazy(() =>
  import("../pages/MeetingDetailPage").then((m) => ({ default: m.MeetingDetailPage })));
const NotificationPreferencesPage = lazy(() =>
  import("../pages/NotificationPreferencesPage").then((m) => ({ default: m.NotificationPreferencesPage })));
const ProvidersPage = lazy(() => import("../pages/ProvidersPage").then((m) => ({ default: m.ProvidersPage })));
const ProviderDetailPage = lazy(() =>
  import("../pages/ProviderDetailPage").then((m) => ({ default: m.ProviderDetailPage })));
const VotingsPage = lazy(() => import("../pages/VotingsPage").then((m) => ({ default: m.VotingsPage })));
const VotingDetailPage = lazy(() =>
  import("../pages/VotingDetailPage").then((m) => ({ default: m.VotingDetailPage })));
const ChatPage = lazy(() => import("../pages/ChatPage").then((m) => ({ default: m.ChatPage })));
const MembersPage = lazy(() => import("../pages/MembersPage").then((m) => ({ default: m.MembersPage })));
const NotFoundPage = lazy(() => import("../pages/NotFoundPage").then((m) => ({ default: m.NotFoundPage })));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const baseUrl = import.meta.env.BASE_URL;
const basename = baseUrl === "/" ? undefined : baseUrl.replace(/\/$/, "");

export function App() {
  const bootstrapped = useRef(false);

  useEffect(() => {
    if (bootstrapped.current) {
      return;
    }
    bootstrapped.current = true;
    void useAuthStore.getState().bootstrap();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename={basename}>
        <Suspense fallback={<FullPageLoader />}>
          <Routes>
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/invitation/:token" element={<InvitationPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<HomeRedirect />} />
              <Route path="/communities" element={<CommunitySelectPage />} />
              <Route path="/organizations" element={<OrganizationsPage />} />
              <Route path="/organizations/:organizationId" element={<OrganizationDetailPage />} />
              <Route path="/communities/:communityId" element={<AppLayout />}>
                <Route index element={<Navigate to="dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="structure" element={<StructurePage />} />
                <Route path="units/:unitId" element={<UnitDetailPage />} />
                <Route path="finance" element={<FinancePage />} />
                <Route path="banking" element={<BankingPage />} />
                <Route path="incidents" element={<IncidentsPage />} />
                <Route path="incidents/:incidentId" element={<IncidentDetailPage />} />
                <Route path="documents" element={<DocumentsPage />} />
                <Route path="documents/:documentId" element={<DocumentDetailPage />} />
                <Route path="meetings" element={<MeetingsPage />} />
                <Route path="meetings/:meetingId" element={<MeetingDetailPage />} />
                <Route path="notification-preferences" element={<NotificationPreferencesPage />} />
                <Route path="providers" element={<ProvidersPage />} />
                <Route path="providers/:providerId" element={<ProviderDetailPage />} />
                <Route path="votings" element={<VotingsPage />} />
                <Route path="votings/:votingId" element={<VotingDetailPage />} />
                <Route path="chat" element={<ChatPage />} />
                <Route path="members" element={<MembersPage />} />
              </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
