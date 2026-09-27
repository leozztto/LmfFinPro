import type { AccountScope } from '@/features/accounts/types'
import type { Currency } from '@/shared/format/currency'

export interface NetWorthPoint {
  /** "YYYY-MM" */
  month: string
  cash: number
  investments: number
  debts: number
  netWorth: number
}

export interface NetWorthAccountRow {
  accountId: number
  name: string
  scope: AccountScope
  /** Na moeda da conta. */
  balance: number
  currency: Currency
  /** Pela última cotação. */
  balanceInBrl: number
}

/** Valores na moeda da conta; os `...InBrl`, em reais pela última cotação. */
export interface NetWorthInvestmentRow {
  accountId: number
  name: string
  scope: AccountScope
  /** Saldo inicial + aplicações − resgates. */
  invested: number
  currentValue: number
  /** Valor atual − aplicado. */
  gain: number
  /** Fração (0.05 = 5%); null quando o aplicado não é positivo. */
  gainRate: number | null
  lastValuationDate: string | null
  currency: Currency
  currentValueInBrl: number
  gainInBrl: number
}

export type DebtType = 'FINANCING' | 'LOAN' | 'CREDIT_CARD' | 'OTHER'

export interface NetWorthDebtRow {
  debtId: number
  name: string
  type: DebtType
  creditor: string | null
  currentBalance: number
  lastBalanceDate: string | null
}

/** Tudo calculado no backend; a tela só exibe. */
export interface NetWorth {
  current: NetWorthPoint
  /** Patrimônio atual − o do fim do mês anterior; null com um mês só. */
  changeFromPreviousMonth: number | null
  investmentGain: number
  history: NetWorthPoint[]
  accounts: NetWorthAccountRow[]
  investments: NetWorthInvestmentRow[]
  debts: NetWorthDebtRow[]
}

export interface DebtCreateInput {
  name: string
  type: DebtType
  creditor: string | null
  balance: number
  balanceDate: string
}

export interface DebtUpdateInput {
  name: string
  type: DebtType
  creditor: string | null
}

export interface DebtBalance {
  id: number
  debtId: number
  balanceDate: string
  balance: number
}

export interface DebtBalanceInput {
  balanceDate: string
  balance: number
}

export const DEBT_TYPE_LABELS: Record<DebtType, string> = {
  FINANCING: 'Financiamento',
  LOAN: 'Empréstimo',
  CREDIT_CARD: 'Cartão de crédito',
  OTHER: 'Outra',
}
