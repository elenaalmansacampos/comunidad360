import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type {
  AllocationMethod,
  Assessment,
  AssessmentCharge,
  AssessmentType,
  Budget,
  BudgetItemType,
  FinanceSummary,
  FiscalYear,
  Invoice,
  LedgerEntry,
  LedgerEntryType,
  Payment,
  PaymentMethod,
} from "../../types/api";

function financeKey(communityId: string | undefined) {
  return ["finance", communityId] as const;
}

function invalidateFinance(queryClient: ReturnType<typeof useQueryClient>, communityId: string | undefined) {
  return queryClient.invalidateQueries({ queryKey: financeKey(communityId) });
}

export function useFiscalYears(communityId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "fiscal-years"],
    queryFn: () => api.get<FiscalYear[]>(`/communities/${communityId}/finance/fiscal-years`),
    enabled: Boolean(communityId) && enabled,
  });
}

export function useFinanceSummary(communityId: string | undefined, fiscalYearId: string | undefined,
                                  enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "summary", fiscalYearId ?? "current"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<FinanceSummary>(`/communities/${communityId}/finance/summary${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useAssessments(communityId: string | undefined, fiscalYearId: string | undefined,
                               enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "assessments", fiscalYearId ?? "all"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<Assessment[]>(`/communities/${communityId}/finance/assessments${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useCharges(communityId: string | undefined, fiscalYearId: string | undefined,
                           enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "charges", fiscalYearId ?? "current"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<AssessmentCharge[]>(`/communities/${communityId}/finance/charges${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useInvoices(communityId: string | undefined, fiscalYearId: string | undefined,
                            enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "invoices", fiscalYearId ?? "all"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<Invoice[]>(`/communities/${communityId}/finance/invoices${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function usePayments(communityId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "payments"],
    queryFn: () => api.get<Payment[]>(`/communities/${communityId}/finance/payments`),
    enabled: Boolean(communityId) && enabled,
  });
}

export function useLedgerEntries(communityId: string | undefined, fiscalYearId: string | undefined,
                                 enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "ledger", fiscalYearId ?? "all"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<LedgerEntry[]>(`/communities/${communityId}/finance/ledger-entries${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useBudgets(communityId: string | undefined, fiscalYearId: string | undefined,
                           enabled = true) {
  return useQuery({
    queryKey: [...financeKey(communityId), "budgets", fiscalYearId ?? "all"],
    queryFn: () => {
      const query = fiscalYearId ? `?fiscalYearId=${fiscalYearId}` : "";
      return api.get<Budget[]>(`/communities/${communityId}/finance/budgets${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useCreateFiscalYear(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { year: number; startDate: string; endDate: string }) =>
      api.post<FiscalYear>(`/communities/${communityId}/finance/fiscal-years`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCloseFiscalYear(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (fiscalYearId: string) =>
      api.post<FiscalYear>(`/communities/${communityId}/finance/fiscal-years/${fiscalYearId}/close`),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCreateAssessment(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      fiscalYearId: string;
      concept: string;
      type: AssessmentType;
      periodStart: string;
      periodEnd: string;
      dueDate: string;
      totalAmount: string;
      allocationMethod: AllocationMethod;
    }) => api.post<Assessment>(`/communities/${communityId}/finance/assessments`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useIssueAssessment(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (assessmentId: string) =>
      api.post<Assessment>(`/communities/${communityId}/finance/assessments/${assessmentId}/issue`),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCreatePayment(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      assessmentChargeId?: string;
      invoiceId?: string;
      amount: string;
      paymentDate: string;
      method: PaymentMethod;
      reference?: string;
    }) => api.post<Payment>(`/communities/${communityId}/finance/payments`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCreateInvoice(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      fiscalYearId: string;
      providerName: string;
      number: string;
      issueDate: string;
      dueDate?: string;
      subtotal: string;
      taxAmount: string;
      total: string;
    }) => api.post<Invoice>(`/communities/${communityId}/finance/invoices`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCreateLedgerEntry(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      fiscalYearId: string;
      entryDate: string;
      type: LedgerEntryType;
      category: string;
      description: string;
      amount: string;
    }) => api.post<LedgerEntry>(`/communities/${communityId}/finance/ledger-entries`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function usePostLedgerEntry(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId: string) =>
      api.post<LedgerEntry>(`/communities/${communityId}/finance/ledger-entries/${entryId}/post`),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useReverseLedgerEntry(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId: string) =>
      api.post<LedgerEntry>(`/communities/${communityId}/finance/ledger-entries/${entryId}/reverse`),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useCreateBudget(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { fiscalYearId: string; notes?: string }) =>
      api.post<Budget>(`/communities/${communityId}/finance/budgets`, payload),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useAddBudgetItem(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      budgetId: string;
      type: BudgetItemType;
      category: string;
      description: string;
      plannedAmount: string;
    }) => api.post<Budget>(`/communities/${communityId}/finance/budgets/${payload.budgetId}/items`, {
      type: payload.type,
      category: payload.category,
      description: payload.description,
      plannedAmount: payload.plannedAmount,
    }),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}

export function useApproveBudget(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (budgetId: string) =>
      api.post<Budget>(`/communities/${communityId}/finance/budgets/${budgetId}/approve`),
    onSuccess: () => invalidateFinance(queryClient, communityId),
  });
}
