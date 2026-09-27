import { httpClient } from '@/shared/api/httpClient'
import type { DebtBalance, DebtBalanceInput, DebtCreateInput, DebtUpdateInput, NetWorth } from '../types'

export const netWorthApi = {
  get: (months: number) => httpClient.get<NetWorth>(`/net-worth?months=${months}`),
  createDebt: (input: DebtCreateInput) => httpClient.post<unknown, DebtCreateInput>('/debts', input),
  updateDebt: (id: number, input: DebtUpdateInput) => httpClient.put<unknown, DebtUpdateInput>(`/debts/${id}`, input),
  removeDebt: (id: number) => httpClient.delete(`/debts/${id}`),
  listDebtBalances: (debtId: number) => httpClient.get<DebtBalance[]>(`/debts/${debtId}/balances`),
  saveDebtBalance: (debtId: number, input: DebtBalanceInput) =>
    httpClient.post<DebtBalance, DebtBalanceInput>(`/debts/${debtId}/balances`, input),
  removeDebtBalance: (debtId: number, balanceId: number) =>
    httpClient.delete(`/debts/${debtId}/balances/${balanceId}`),
}
