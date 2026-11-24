export type Role =
  | "SUPER_ADMIN"
  | "COMMUNITY_ADMIN"
  | "PRESIDENT"
  | "SECRETARY"
  | "TREASURER"
  | "OWNER"
  | "TENANT"
  | "PROVIDER"
  | "VIEWER"
  | "VECINO";

export type Language = "es" | "ca" | "en";

export interface CommunitySummary {
  id: string;
  name: string;
  roles: Role[];
  status: string;
}

export type OrganizationStatus = "ACTIVE" | "INACTIVE";
export type OrganizationMemberRole = "ORG_ADMIN" | "ORG_STAFF";
export type OrganizationMemberStatus = "ACTIVE" | "INACTIVE";

export interface Organization {
  id: string;
  name: string;
  taxId?: string | null;
  status: OrganizationStatus;
  role: OrganizationMemberRole;
  communityCount: number;
  createdAt: string;
}

export interface OrganizationCommunity {
  id: string;
  name: string;
  status: string;
}

export interface OrganizationMember {
  userId: string;
  fullName: string;
  email: string;
  role: OrganizationMemberRole;
  status: OrganizationMemberStatus;
  createdAt: string;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  preferredLanguage: Language;
  status: string;
  emailVerifiedAt: string | null;
  memberships: CommunitySummary[];
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface DashboardFinance {
  fiscalYear: number;
  scope: "COMMUNITY" | "OWN";
  currency: string;
  balance: number | string;
  income: number | string;
  expense: number | string;
  pendingCharges: number | string;
  invoicesPending: number | string;
}

export interface DashboardIncident {
  id: string;
  title: string;
  status: IncidentStatus;
  priority: IncidentPriority;
  unitCode?: string | null;
  createdAt: string;
}

export interface DashboardMeeting {
  id: string;
  title: string;
  status: MeetingStatus;
  scheduledStart: string;
  location?: string | null;
}

export interface DashboardMonth {
  month: number;
  income: number | string;
  expense: number | string;
}

export interface DashboardComparison {
  currentMonth: number;
  previousMonth: number;
  currentIncome: number | string;
  currentExpense: number | string;
  previousIncome?: number | string | null;
  previousExpense?: number | string | null;
  incomeDelta?: number | string | null;
  expenseDelta?: number | string | null;
}

export interface Dashboard {
  community: CommunitySummary;
  roles: Role[];
  activeMembers: number;
  openIncidents: number;
  finance?: DashboardFinance | null;
  recentIncidents: DashboardIncident[];
  upcomingMeetings: DashboardMeeting[];
  monthlyFinance: DashboardMonth[];
  comparison?: DashboardComparison | null;
  canViewReports: boolean;
}

export type ReportFrequency = "DAILY" | "WEEKLY" | "MONTHLY";
export type ReportRunStatus = "SENT" | "FAILED" | "SKIPPED";

export interface ReportSchedule {
  id: string;
  name: string;
  frequency: ReportFrequency;
  recipients: string[];
  enabled: boolean;
  lastRunAt?: string | null;
  nextRunAt: string;
  lastStatus?: ReportRunStatus | null;
  version: number;
}

export interface Member {
  membershipId: string;
  userId: string;
  fullName: string;
  email: string;
  status: string;
  roles: Role[];
}

export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  code?: string;
  messageKey?: string;
  params?: Record<string, unknown>;
  detail?: string;
  traceId?: string;
}

export type UnitType = "VIVIENDA" | "LOCAL" | "GARAJE" | "TRASTERO" | "ELEMENTO_COMUN";
export type UnitStatus = "ACTIVE" | "INACTIVE";

export interface Building {
  id: string;
  name: string;
  address?: string | null;
  city?: string | null;
  postalCode?: string | null;
  floors?: number | null;
  notes?: string | null;
  archivedAt?: string | null;
  version?: number;
}

export interface Unit {
  id: string;
  buildingId?: string | null;
  buildingName?: string | null;
  code: string;
  type: UnitType;
  floor?: string | null;
  door?: string | null;
  builtArea?: number | string | null;
  coefficient: number | string;
  status: UnitStatus;
  archivedAt?: string | null;
  version?: number;
}

export interface CommonElement {
  id: string;
  buildingId?: string | null;
  buildingName?: string | null;
  name: string;
  type?: string | null;
  description?: string | null;
  archivedAt?: string | null;
}

export interface StructureSummary {
  coefficientSum: number | string;
  tolerance: number | string;
  valid: boolean;
  totalUnits: number;
  activeUnits: number;
  buildings: number;
  commonElements: number;
}

export type OwnershipType = "OWNER" | "CO_OWNER" | "USUFRUCTUARY" | "NAKED_OWNER";
export type TenancyStatus = "ACTIVE" | "ENDED" | "REVOKED";

export interface Ownership {
  id: string;
  userId: string;
  fullName?: string | null;
  email?: string | null;
  ownershipType: OwnershipType;
  sharePercentage: number | string;
  validFrom: string;
  validTo?: string | null;
  active: boolean;
  version: number;
}

export interface Tenancy {
  id: string;
  userId: string;
  fullName?: string | null;
  email?: string | null;
  validFrom: string;
  validTo?: string | null;
  status: TenancyStatus;
  active: boolean;
  version: number;
}

export type FiscalYearStatus = "OPEN" | "CLOSED";
export type BudgetStatus = "DRAFT" | "APPROVED" | "CLOSED";
export type BudgetItemType = "INCOME" | "EXPENSE";
export type AssessmentType = "ORDINARY" | "EXTRAORDINARY";
export type AllocationMethod = "COEFFICIENT" | "EQUAL" | "CUSTOM";
export type AssessmentStatus = "DRAFT" | "ISSUED" | "CLOSED" | "CANCELLED";
export type ChargeStatus = "PENDING" | "PARTIAL" | "PAID" | "CANCELLED";
export type InvoiceStatus = "DRAFT" | "REGISTERED" | "PAID" | "CANCELLED";
export type PaymentMethod = "BANK_TRANSFER" | "CASH" | "CARD" | "OTHER";
export type PaymentStatus = "REGISTERED" | "RECONCILED" | "CANCELLED";
export type LedgerEntryType = "INCOME" | "EXPENSE" | "TRANSFER" | "ADJUSTMENT" | "REVERSAL";
export type LedgerEntryStatus = "DRAFT" | "POSTED" | "VOID";

export interface FiscalYear {
  id: string;
  year: number;
  startDate: string;
  endDate: string;
  status: FiscalYearStatus;
  closedAt?: string | null;
  version: number;
}

export interface BudgetItem {
  id: string;
  type: BudgetItemType;
  category: string;
  description: string;
  plannedAmount: number | string;
  position: number;
}

export interface Budget {
  id: string;
  fiscalYearId: string;
  status: BudgetStatus;
  totalIncome: number | string;
  totalExpense: number | string;
  notes?: string | null;
  approvedAt?: string | null;
  items: BudgetItem[];
  version: number;
}

export interface Assessment {
  id: string;
  fiscalYearId: string;
  concept: string;
  type: AssessmentType;
  periodStart: string;
  periodEnd: string;
  dueDate: string;
  totalAmount: number | string;
  allocationMethod: AllocationMethod;
  status: AssessmentStatus;
  chargesCount: number;
  issuedAt?: string | null;
  version: number;
}

export interface AssessmentCharge {
  id: string;
  assessmentId: string;
  unitId: string;
  unitCode?: string | null;
  amount: number | string;
  coefficientSnapshot: number | string;
  paidAmount: number | string;
  status: ChargeStatus;
  dueDate?: string | null;
  version: number;
}

export interface Invoice {
  id: string;
  fiscalYearId: string;
  providerName: string;
  number: string;
  issueDate: string;
  dueDate?: string | null;
  subtotal: number | string;
  taxAmount: number | string;
  total: number | string;
  paidAmount: number | string;
  currency: string;
  status: InvoiceStatus;
  version: number;
}

export interface Payment {
  id: string;
  unitId?: string | null;
  assessmentChargeId?: string | null;
  invoiceId?: string | null;
  amount: number | string;
  paymentDate: string;
  method: PaymentMethod;
  reference?: string | null;
  status: PaymentStatus;
  version: number;
}

export interface LedgerEntry {
  id: string;
  fiscalYearId: string;
  entryNumber: number;
  entryDate: string;
  type: LedgerEntryType;
  category: string;
  description: string;
  amount: number | string;
  status: LedgerEntryStatus;
  reversalOfId?: string | null;
  postedAt?: string | null;
  version: number;
}

export interface FinanceSummary {
  fiscalYearId: string;
  fiscalYear: number;
  scope: "COMMUNITY" | "OWN";
  currency: string;
  balance: number | string;
  income: number | string;
  expense: number | string;
  pendingCharges: number | string;
  invoicesPending: number | string;
}

export type BankAccountStatus = "ACTIVE" | "INACTIVE";
export type BankAccountSource = "MANUAL" | "CSV" | "PSD2";
export type BankImportStatus = "PROCESSING" | "COMPLETED" | "FAILED";
export type BankTransactionStatus = "UNRECONCILED" | "RECONCILED" | "IGNORED";

export interface BankAccount {
  id: string;
  name: string;
  bankName?: string | null;
  ibanLast4?: string | null;
  ibanMasked?: string | null;
  currency: string;
  status: BankAccountStatus;
  source: BankAccountSource;
  version: number;
}

export interface BankImportBatch {
  id: string;
  bankAccountId: string;
  fileName: string;
  importedAt: string;
  rowCount: number;
  successCount: number;
  duplicateCount: number;
  errorCount: number;
  status: BankImportStatus;
}

export interface BankTransaction {
  id: string;
  bankAccountId: string;
  valueDate: string;
  bookingDate?: string | null;
  description: string;
  amount: number | string;
  balanceAfter?: number | string | null;
  currency: string;
  status: BankTransactionStatus;
  reconciliationId?: string | null;
  paymentId?: string | null;
  ledgerEntryId?: string | null;
  version: number;
}

export interface Reconciliation {
  id: string;
  bankTransactionId: string;
  paymentId?: string | null;
  ledgerEntryId?: string | null;
  matchedAmount: number | string;
  matchedAt: string;
  notes?: string | null;
}

export type IncidentPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type IncidentStatus = "OPEN" | "IN_PROGRESS" | "WAITING_PROVIDER" | "RESOLVED" | "CLOSED" | "CANCELLED";

export interface Incident {
  id: string;
  title: string;
  description: string;
  category: string;
  priority: IncidentPriority;
  status: IncidentStatus;
  buildingId?: string | null;
  unitId?: string | null;
  unitCode?: string | null;
  commonElementId?: string | null;
  reportedByUserId?: string | null;
  reportedByName?: string | null;
  assignedToUserId?: string | null;
  assignedToName?: string | null;
  resolvedAt?: string | null;
  closedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface IncidentComment {
  id: string;
  body: string;
  authorName?: string | null;
  internal: boolean;
  createdAt: string;
}

export interface IncidentHistory {
  id: string;
  fromStatus?: IncidentStatus | null;
  toStatus: IncidentStatus;
  changedByName?: string | null;
  changedAt: string;
  note?: string | null;
}

export interface IncidentAttachment {
  id: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedByName?: string | null;
  createdAt: string;
  downloadUrl: string;
}

export interface IncidentDetail {
  incident: Incident;
  comments: IncidentComment[];
  attachments: IncidentAttachment[];
  history: IncidentHistory[];
}

export type DocumentCategory = "ACTA" | "ESTATUTO" | "CONTRATO" | "FACTURA" | "PRESUPUESTO" | "INFORME" | "CERTIFICADO" | "OTRO";
export type DocumentVisibility = "COMMUNITY" | "BOARD" | "OWNERS" | "ROLE" | "PRIVATE";
export type DocumentStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type DocumentScanStatus = "PENDING" | "CLEAN" | "INFECTED" | "ERROR";

export interface CommunityDocument {
  id: string;
  category: DocumentCategory;
  title: string;
  description?: string | null;
  visibility: DocumentVisibility;
  status: DocumentStatus;
  ownerUserId?: string | null;
  currentVersionNumber?: number | null;
  fileName?: string | null;
  mimeType?: string | null;
  sizeBytes?: number | null;
  createdAt: string;
  updatedAt: string;
  downloadUrl?: string | null;
  version: number;
}

export interface DocumentVersion {
  id: string;
  versionNumber: number;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedByName?: string | null;
  uploadedAt: string;
  scanStatus: DocumentScanStatus;
  downloadUrl?: string | null;
}

export interface DocumentDetail {
  document: CommunityDocument;
  versions: DocumentVersion[];
}

export type MeetingType = "ORDINARY" | "EXTRAORDINARY";
export type MeetingStatus = "DRAFT" | "CONVENED" | "HELD" | "CANCELLED";
export type AttendanceType = "IN_PERSON" | "ONLINE" | "REPRESENTED" | "ABSENT";

export interface Meeting {
  id: string;
  title: string;
  type: MeetingType;
  scheduledStart: string;
  scheduledEnd?: string | null;
  location?: string | null;
  status: MeetingStatus;
  convocationDocumentId?: string | null;
  minutesDocumentId?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface MeetingAgendaItem {
  id: string;
  position: number;
  title: string;
  description?: string | null;
}

export interface MeetingAttendance {
  id: string;
  membershipId: string;
  memberName?: string | null;
  attendanceType: AttendanceType;
  representedByMembershipId?: string | null;
  representedByName?: string | null;
  coefficientSnapshot: number | string;
  recordedAt: string;
}

export interface MeetingAgreement {
  id: string;
  agendaItemId?: string | null;
  title: string;
  description?: string | null;
  votesFor: number;
  votesAgainst: number;
  abstentions: number;
  coefficientFor: number | string;
  coefficientAgainst: number | string;
  coefficientAbstention: number | string;
  adopted?: boolean | null;
  createdAt: string;
}

export interface MeetingDetail {
  meeting: Meeting;
  agenda: MeetingAgendaItem[];
  attendance: MeetingAttendance[];
  agreements: MeetingAgreement[];
}

export type NotificationChannel = "IN_APP" | "EMAIL" | "PUSH";
export type NotificationStatus = "PENDING" | "SENT" | "FAILED" | "READ";

export interface AppNotification {
  id: string;
  communityId?: string | null;
  type: string;
  category: string;
  templateKey: string;
  params: Record<string, unknown>;
  locale: string;
  channel: NotificationChannel;
  status: NotificationStatus;
  readAt?: string | null;
  createdAt: string;
  sentAt?: string | null;
}

export interface NotificationPreference {
  category: string;
  channel: NotificationChannel;
  enabled: boolean;
}

export type ProviderStatus = "ACTIVE" | "INACTIVE";

export interface Provider {
  id: string;
  name: string;
  taxId?: string | null;
  category?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  status: ProviderStatus;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ProviderContact {
  id: string;
  providerId: string;
  name: string;
  role?: string | null;
  email?: string | null;
  phone?: string | null;
  primaryContact: boolean;
  createdAt: string;
  updatedAt: string;
}

export type ProviderContractStatus = "DRAFT" | "ACTIVE" | "EXPIRED" | "CANCELLED";

export interface ProviderContract {
  id: string;
  title: string;
  description?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  amount?: number | null;
  status: ProviderContractStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type ProviderBudgetStatus = "RECEIVED" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export interface ProviderBudget {
  id: string;
  title: string;
  description?: string | null;
  amount: number;
  validUntil?: string | null;
  status: ProviderBudgetStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ProviderNote {
  id: string;
  body: string;
  createdAt: string;
}

export interface ProviderHistoryEntry {
  type: "CONTRACT" | "BUDGET" | "NOTE";
  referenceId: string;
  title?: string | null;
  description?: string | null;
  status?: string | null;
  amount?: number | null;
  date: string;
}

export type VotingStatus = "DRAFT" | "OPEN" | "CLOSED" | "CANCELLED";

export interface Voting {
  id: string;
  title: string;
  description?: string | null;
  status: VotingStatus;
  opensAt?: string | null;
  closesAt: string;
  legalNotice: string;
  totalEligible: number;
  hasVoted: boolean;
  createdAt: string;
  version: number;
}

export interface VotingOption {
  id: string;
  label: string;
  position: number;
}

export interface VotingOptionResult {
  optionId: string;
  label: string;
  votes: number;
  coefficient: number | string;
  percentage: number | string;
}

export interface VotingResults {
  totalEligible: number;
  participatingEligible: number;
  totalCoefficient: number | string;
  participatingCoefficient: number | string;
  quorumPercentage: number | string;
  options: VotingOptionResult[];
}

export interface VotingDetail {
  voting: Voting;
  options: VotingOption[];
  results?: VotingResults | null;
  delegations?: VotingDelegation[];
}

export interface VotingDelegation {
  membershipId: string;
  userId?: string | null;
  name?: string | null;
  hasVoted: boolean;
}

export type ChatChannelType = "GENERAL" | "THEMATIC" | "PRIVATE";

export interface ChatChannel {
  id: string;
  type: ChatChannelType;
  name: string;
  description?: string | null;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  senderUserId?: string | null;
  senderName?: string | null;
  body: string;
  createdAt: string;
}

export type LostItemType = "LOST" | "FOUND";
export type LostItemStatus = "OPEN" | "RESOLVED" | "ARCHIVED";

export interface LostItem {
  id: string;
  title: string;
  description?: string | null;
  type: LostItemType;
  location?: string | null;
  eventDate?: string | null;
  contact?: string | null;
  status: LostItemStatus;
  createdByUserId?: string | null;
  createdByName?: string | null;
  photoUrl?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  notes?: LostItemNote[];
  attachments?: LostItemAttachment[];
}

export interface LostItemNote {
  id: string;
  itemId: string;
  body: string;
  authorUserId?: string | null;
  authorName?: string | null;
  createdAt: string;
}

export interface LostItemAttachment {
  id: string;
  itemId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedByUserId?: string | null;
  uploadedByName?: string | null;
  downloadUrl: string;
  createdAt: string;
}

export type BoardNoteColor = "YELLOW" | "PINK" | "BLUE" | "GREEN" | "ORANGE";

export interface BoardNote {
  id: string;
  title: string;
  body: string;
  color: BoardNoteColor;
  pinned: boolean;
  commentCount: number;
  createdByUserId?: string | null;
  createdByName?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface BoardNoteComment {
  id: string;
  noteId: string;
  body: string;
  authorUserId?: string | null;
  authorName?: string | null;
  createdAt: string;
}

export interface Invitation {
  id: string;
  email: string;
  role: Role;
  expiresAt: string;
  acceptedAt?: string | null;
  status: string;
  devToken?: string | null;
}

export type WorkPermitStatus = "REQUESTED" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface WorkPermit {
  id: string;
  title: string;
  description?: string | null;
  location?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  status: WorkPermitStatus;
  createdByUserId?: string | null;
  createdByName?: string | null;
  reviewedByUserId?: string | null;
  reviewedByName?: string | null;
  reviewedAt?: string | null;
  reviewNotes?: string | null;
  fileUrl?: string | null;
  fileName?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface VecinoAccess {
  email: string;
  password: string;
  role: string;
}

export type AuthorizationScope = "VOTE" | "WORK_PERMIT" | "INCIDENT" | "BOARD" | "LOST_FOUND";

export type AuthorizationStatus = "ACTIVE" | "REVOKED";

export interface Authorization {
  id: string;
  grantorMembershipId: string;
  grantorUserId?: string | null;
  grantorName?: string | null;
  granteeMembershipId: string;
  granteeUserId?: string | null;
  granteeName?: string | null;
  scope: AuthorizationScope;
  status: AuthorizationStatus;
  effective: boolean;
  validFrom: string;
  validUntil?: string | null;
  notes?: string | null;
  createdAt: string;
  revokedAt?: string | null;
}

export interface CreateAuthorizationPayload {
  granteeMembershipId: string;
  scope: AuthorizationScope;
  validFrom?: string;
  validUntil?: string;
  notes?: string;
}
