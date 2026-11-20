export const demoEnabled = import.meta.env.VITE_DEMO_MODE === "true";

const community = {
  id: "demo-community",
  name: "Comunidad Demo 360",
  roles: ["COMMUNITY_ADMIN"],
  status: "ACTIVE",
};

const now = Date.now();
const meetingStart = new Date(now + 3 * 86_400_000).toISOString();
const incidentCreated = new Date(now - 86_400_000).toISOString();

const meeting = {
  id: "demo-meeting-1",
  title: "Junta ordinaria",
  type: "ORDINARY",
  scheduledStart: meetingStart,
  scheduledEnd: null,
  location: "Salón de actos",
  status: "CONVENED",
  convocationDocumentId: null,
  minutesDocumentId: null,
  createdAt: incidentCreated,
  updatedAt: incidentCreated,
  version: 0,
};

const incident = {
  id: "demo-incident-1",
  title: "Fuga en garaje",
  description: "Mancha de agua junto a la plaza 12.",
  category: "FONTANERIA",
  priority: "HIGH",
  status: "IN_PROGRESS",
  buildingId: null,
  unitId: null,
  unitCode: "1A",
  commonElementId: null,
  reportedByUserId: "demo-owner",
  reportedByName: "Propietario Demo",
  assignedToUserId: null,
  assignedToName: null,
  resolvedAt: null,
  closedAt: null,
  createdAt: incidentCreated,
  updatedAt: incidentCreated,
  version: 0,
};

const monthly = Array.from({ length: 12 }, (_, index) => ({
  month: index + 1,
  income: index < 6 ? String(1200 + index * 80) : String(900 + index * 40),
  expense: String(600 + index * 35),
}));

const user = {
  id: "demo-admin",
  email: "admin@comunidad360.local",
  fullName: "Administradora Demo",
  preferredLanguage: "es",
  status: "ACTIVE",
  emailVerifiedAt: incidentCreated,
  memberships: [community],
};

const members = [
  { membershipId: "demo-admin-membership", userId: "demo-admin", fullName: "Administradora Demo", email: user.email, status: "ACTIVE", roles: ["COMMUNITY_ADMIN"] },
  { membershipId: "demo-owner-membership", userId: "demo-owner", fullName: "Propietario Demo", email: "propietario@example.com", status: "ACTIVE", roles: ["OWNER"] },
  { membershipId: "demo-tenant-membership", userId: "demo-tenant", fullName: "Inquilino Demo", email: "inquilino@example.com", status: "ACTIVE", roles: ["TENANT"] },
];

const dashboard = {
  community,
  roles: ["COMMUNITY_ADMIN"],
  activeMembers: 24,
  openIncidents: 3,
  finance: {
    fiscalYear: 2026,
    scope: "COMMUNITY",
    currency: "EUR",
    balance: "8420.50",
    income: "18600.00",
    expense: "10179.50",
    pendingCharges: "1240.00",
    invoicesPending: "430.00",
  },
  recentIncidents: [
    { id: incident.id, title: incident.title, status: incident.status, priority: incident.priority, unitCode: "1A", createdAt: incident.createdAt },
    { id: "demo-incident-2", title: "Luz fundida en portal", status: "OPEN", priority: "MEDIUM", unitCode: null, createdAt: incidentCreated },
  ],
  upcomingMeetings: [
    { id: meeting.id, title: meeting.title, status: meeting.status, scheduledStart: meeting.scheduledStart, location: meeting.location },
  ],
  monthlyFinance: monthly,
  canViewReports: false,
};

const chatChannel = {
  id: "demo-general",
  type: "GENERAL",
  name: "General",
  description: "Canal general de la comunidad",
  createdAt: incidentCreated,
};

const voting = {
  id: "demo-voting-1",
  title: "¿Instalar placas solares?",
  description: "Consulta informativa sobre la instalación en cubierta.",
  status: "CLOSED",
  opensAt: incidentCreated,
  closesAt: incidentCreated,
  legalNotice: "Votación consultiva no vinculante jurídicamente.",
  totalEligible: 24,
  hasVoted: true,
  createdAt: incidentCreated,
  version: 1,
};

const notificationPreferences = ["MEETINGS", "INCIDENTS", "FINANCE", "SYSTEM"].flatMap((category) =>
  ["IN_APP", "EMAIL"].map((channel) => ({ category, channel, enabled: true })),
);

function json<T>(value: T): T {
  return value;
}

export async function demoRequest<T>(path: string, method = "GET"): Promise<T> {
  const clean = path.split("?")[0];
  if (clean === "/auth/login" || clean === "/auth/refresh") {
    return json({ accessToken: "demo-token", tokenType: "Bearer", expiresIn: 3600 } as T);
  }
  if (clean === "/auth/logout") return json({} as T);
  if (clean === "/me") return json(user as T);
  if (clean === "/communities") return json([community] as T);
  if (clean === "/notifications") return json([] as T);
  if (clean === "/notifications/unread-count") return json({ count: 0 } as T);
  if (clean === "/me/notification-preferences") return json(notificationPreferences as T);
  if (clean.endsWith("/dashboard")) return json(dashboard as T);
  if (clean.endsWith("/members")) return json(members as T);
  if (clean.endsWith("/structure/summary")) {
    return json({ coefficientSum: "100.000000", tolerance: "0.010000", valid: true, totalUnits: 24, activeUnits: 24, buildings: 2, commonElements: 4 } as T);
  }
  if (clean.endsWith("/incidents")) return json([incident] as T);
  if (clean.endsWith("/incidents/demo-incident-1")) {
    return json({ incident, comments: [], attachments: [], history: [] } as T);
  }
  if (clean.endsWith("/meetings")) return json([meeting] as T);
  if (clean.endsWith("/meetings/demo-meeting-1")) {
    return json({
      meeting,
      agenda: [{ id: "demo-agenda-1", position: 1, title: "Aprobación de cuentas", description: "Ejercicio 2026" }],
      attendance: [],
      agreements: [],
    } as T);
  }
  if (clean.endsWith("/votings")) return json([voting] as T);
  if (clean.endsWith("/votings/demo-voting-1")) {
    return json({
      voting,
      options: [{ id: "demo-option-1", label: "Sí", position: 1 }, { id: "demo-option-2", label: "No", position: 2 }],
      results: {
        totalEligible: 24,
        participatingEligible: 16,
        totalCoefficient: "100.000000",
        participatingCoefficient: "66.666600",
        quorumPercentage: "66.67",
        options: [
          { optionId: "demo-option-1", label: "Sí", votes: 11, coefficient: "45.830000", percentage: "68.75" },
          { optionId: "demo-option-2", label: "No", votes: 5, coefficient: "20.836600", percentage: "31.25" },
        ],
      },
    } as T);
  }
  if (clean.endsWith("/chat/channels")) return json([chatChannel] as T);
  if (clean.endsWith("/chat/channels/demo-general/messages")) {
    return json([
      { id: "demo-message-1", senderUserId: "demo-admin", senderName: "Administradora Demo", body: "Bienvenidos a la comunidad.", createdAt: incidentCreated },
    ] as T);
  }
  if (clean.endsWith("/providers")) return json([] as T);
  if (clean.endsWith("/documents")) return json([] as T);
  if (clean.endsWith("/buildings") || clean.endsWith("/units") || clean.endsWith("/common-elements")) {
    return json([] as T);
  }
  if (clean.includes("/finance/")) return json([] as T);
  if (clean.includes("/banking/")) return json([] as T);
  if (method !== "GET") return json({} as T);
  return json([] as T);
}
