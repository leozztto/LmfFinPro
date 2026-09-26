import { useQuery, type QueryClient } from '@tanstack/react-query'
import { recurringTransactionsApi } from '../api/recurringTransactionsApi'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { ACCOUNTS_QUERY_KEY } from '@/features/accounts/hooks/useAccounts'

export const RECURRING_TRANSACTIONS_QUERY_KEY = ['recurring-transactions'] as const

export function useRecurringTransactions() {
  return useQuery({ queryKey: RECURRING_TRANSACTIONS_QUERY_KEY, queryFn: recurringTransactionsApi.list })
}

/**
 * Criar ou editar uma recorrência pode lançar ocorrências vencidas na hora, então transações e
 * saldos das contas também precisam ser recarregados.
 */
export function invalidateRecurringTransactionQueries(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: RECURRING_TRANSACTIONS_QUERY_KEY })
  queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
  queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
}
