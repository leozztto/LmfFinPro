export type AccountType = 'CHECKING' | 'SAVINGS' | 'WALLET' | 'INVESTMENT'

/** Pessoal (PF) ou da empresa (PJ) — base do cálculo de pró-labore. */
export type AccountScope = 'PERSONAL' | 'BUSINESS'

export interface Account {
  id: number
  name: string
  type: AccountType
  initialBalance: number
  currentBalance: number
  createdAt: string
  scope: AccountScope
}

export interface AccountInput {
  name: string
  type: AccountType
  initialBalance: number
  scope: AccountScope
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  WALLET: 'Carteira',
  INVESTMENT: 'Investimento',
}

export const ACCOUNT_SCOPE_LABELS: Record<AccountScope, string> = {
  PERSONAL: 'Pessoal (PF)',
  BUSINESS: 'Empresa (PJ)',
}

/** Valor de mercado de uma conta de investimento num dia (ex.: saldo do extrato da corretora). */
export interface AccountValuation {
  id: number
  accountId: number
  valuationDate: string
  value: number
}

export interface AccountValuationInput {
  valuationDate: string
  value: number
}
