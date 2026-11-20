import { useEffect, useRef } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { useAuthStore } from "../features/auth/authStore";
import { AppLayout } from "../layouts/AppLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { CommunitySelectPage } from "../pages/CommunitySelectPage";
import { DashboardPage } from "../pages/DashboardPage";
import { DocumentDetailPage } from "../pages/DocumentDetailPage";
import { DocumentsPage } from "../pages/DocumentsPage";
import { BankingPage } from "../pages/BankingPage";
import { ChatPage } from "../pages/ChatPage";
import { FinancePage } from "../pages/FinancePage";
import { IncidentDetailPage } from "../pages/IncidentDetailPage";
import { IncidentsPage } from "../pages/IncidentsPage";
import { HomeRedirect } from "../pages/HomeRedirect";
import { InvitationPage } from "../pages/InvitationPage";
import { LoginPage } from "../pages/LoginPage";
import { MeetingDetailPage } from "../pages/MeetingDetailPage";
import { MeetingsPage } from "../pages/MeetingsPage";
import { MembersPage } from "../pages/MembersPage";
import { NotificationPreferencesPage } from "../pages/NotificationPreferencesPage";
import { ProviderDetailPage } from "../pages/ProviderDetailPage";
import { ProvidersPage } from "../pages/ProvidersPage";
import { VotingDetailPage } from "../pages/VotingDetailPage";
import { VotingsPage } from "../pages/VotingsPage";
import { NotFoundPage } from "../pages/NotFoundPage";
import { StructurePage } from "../pages/StructurePage";
import { UnitDetailPage } from "../pages/UnitDetailPage";
import { ProtectedRoute } from "./ProtectedRoute";

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
        <Routes>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/invitation/:token" element={<InvitationPage />} />
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/communities" element={<CommunitySelectPage />} />
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
      </BrowserRouter>
    </QueryClientProvider>
  );
}
