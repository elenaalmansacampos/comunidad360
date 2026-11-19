import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/apiClient";
import type { BankAccount, BankImportBatch, BankTransaction, Reconciliation } from "../../types/api";

function bankingKey(communityId: string | undefined) {
  return ["banking", communityId] as const;
}

function invalidateBanking(queryClient: ReturnType<typeof useQueryClient>, communityId: string | undefined) {
  void queryClient.invalidateQueries({ queryKey: bankingKey(communityId) });
  void queryClient.invalidateQueries({ queryKey: ["finance", communityId] });
}

export function useBankAccounts(communityId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: [...bankingKey(communityId), "accounts"],
    queryFn: () => api.get<BankAccount[]>(`/communities/${communityId}/finance/banking/accounts`),
    enabled: Boolean(communityId) && enabled,
  });
}

export function useBankImports(communityId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: [...bankingKey(communityId), "imports"],
    queryFn: () => api.get<BankImportBatch[]>(`/communities/${communityId}/finance/banking/imports`),
    enabled: Boolean(communityId) && enabled,
  });
}

export function useBankTransactions(communityId: string | undefined, accountId: string | undefined,
                                    enabled = true) {
  return useQuery({
    queryKey: [...bankingKey(communityId), "transactions", accountId ?? "all"],
    queryFn: () => {
      const query = accountId ? `?accountId=${accountId}` : "";
      return api.get<BankTransaction[]>(`/communities/${communityId}/finance/banking/transactions${query}`);
    },
    enabled: Boolean(communityId) && enabled,
  });
}

export function useCreateBankAccount(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { name: string; bankName?: string; iban: string }) =>
      api.post<BankAccount>(`/communities/${communityId}/finance/banking/accounts`, payload),
    onSuccess: () => invalidateBanking(queryClient, communityId),
  });
}

export function useImportCsv(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      api.post<BankImportBatch>(`/communities/${communityId}/finance/banking/imports`, formData),
    onSuccess: () => invalidateBanking(queryClient, communityId),
  });
}

export function useReconcileTransaction(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      transactionId: string;
      paymentId?: string;
      ledgerEntryId?: string;
      matchedAmount: string;
      notes?: string;
    }) => api.post<Reconciliation>(
      `/communities/${communityId}/finance/banking/transactions/${payload.transactionId}/reconcile`,
      {
        paymentId: payload.paymentId,
        ledgerEntryId: payload.ledgerEntryId,
        matchedAmount: payload.matchedAmount,
        notes: payload.notes,
      },
    ),
    onSuccess: () => invalidateBanking(queryClient, communityId),
  });
}

export function useUnreconcile(communityId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reconciliationId: string) =>
      api.delete<void>(`/communities/${communityId}/finance/banking/reconciliations/${reconciliationId}`),
    onSuccess: () => invalidateBanking(queryClient, communityId),
  });
}
