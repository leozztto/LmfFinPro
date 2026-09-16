export type AccountType = 'CHECKING' | 'SAVINGS' | 'WALLET'

export interface Account {
  id: number
  name: string
  type: AccountType
  initialBalance: number
  createdAt: string
}

export interface AccountInput {
  name: string
  type: AccountType
  initialBalance: number
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  WALLET: 'Carteira',
}
