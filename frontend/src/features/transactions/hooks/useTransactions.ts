import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactionsApi'
import type { TransactionListParams } from '../types'

/** Prefixo de todas as consultas de transações: invalidar por ele recarrega qualquer página/filtro. */
export const TRANSACTIONS_QUERY_KEY = ['transactions'] as const

/** Uma página de transações já filtrada pelo backend; mantém a página anterior na tela enquanto a nova carrega. */
export function useTransactions(params: TransactionListParams) {
  return useQuery({
    queryKey: [...TRANSACTIONS_QUERY_KEY, 'list', params],
    queryFn: () => transactionsApi.list(params),
    placeholderData: keepPreviousData,
  })
}
