export type AccountType = 'CHECKING' | 'SAVINGS' | 'WALLET'

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
}

export const ACCOUNT_SCOPE_LABELS: Record<AccountScope, string> = {
  PERSONAL: 'Pessoal (PF)',
  BUSINESS: 'Empresa (PJ)',
}
