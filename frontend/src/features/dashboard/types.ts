export interface DashboardOverview {
  currentBalance: number
  currentMonthIncome: number
  currentMonthExpense: number
  balanceDeltaPercent: number | null
  incomeDeltaPercent: number | null
  expenseDeltaPercent: number | null
  /** Transações pendentes (a receber / a pagar), de qualquer data. */
  pendingIncome: number
  pendingExpense: number
  /** Saldo atual + a receber - a pagar. */
  projectedBalance: number
}

export interface RawMonthlyFlowPoint {
  month: string
  income: number
  expense: number
}

export interface RawBalancePoint {
  month: string
  balance: number
}

export interface RawCashFlowProjectionPoint {
  month: string
  balance: number
  projected: boolean
}

export interface RawBreakdownPoint {
  entityId: number | null
  value: number
}
