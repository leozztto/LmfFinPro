import { httpClient } from '@/shared/api/httpClient'
import type { Page } from '@/shared/api/page'
import type { Transaction, TransactionInput, TransactionListParams, TransactionStatus } from '../types'

function buildListQuery(params: TransactionListParams): string {
  const query = new URLSearchParams()
  query.set('page', String(params.page))
  query.set('size', String(params.size))
  if (params.accountId) query.set('accountId', String(params.accountId))
  if (params.categoryId) query.set('categoryId', String(params.categoryId))
  if (params.clientId) query.set('clientId', String(params.clientId))
  if (params.type) query.set('type', params.type)
  if (params.status) query.set('status', params.status)
  if (params.hasAttachment !== undefined) query.set('hasAttachment', String(params.hasAttachment))
  params.tagNames?.forEach((name) => query.append('tagNames', name))
  if (params.startDate) query.set('startDate', params.startDate)
  if (params.endDate) query.set('endDate', params.endDate)
  return query.toString()
}

export const transactionsApi = {
  list: (params: TransactionListParams) =>
    httpClient.get<Page<Transaction>>(`/transactions?${buildListQuery(params)}`),
  create: (input: TransactionInput) => httpClient.post<Transaction, TransactionInput>('/transactions', input),
  updateStatus: (id: number, status: TransactionStatus) =>
    httpClient.patch<Transaction, { status: TransactionStatus }>(`/transactions/${id}/status`, { status }),
  updateTags: (id: number, tagNames: string[]) =>
    httpClient.put<Transaction, { tagNames: string[] }>(`/transactions/${id}/tags`, { tagNames }),
  remove: (id: number) => httpClient.delete(`/transactions/${id}`),
}
