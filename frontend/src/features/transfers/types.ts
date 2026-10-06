import type { Currency } from '@/shared/format/currency'

export interface Transfer {
  id: number
  fromAccountId: number
  toAccountId: number
  /** Valor que sai da origem, na moeda dela. */
  amount: number
  transferDate: string
  description: string | null
  fromTransactionId: number
  toTransactionId: number
  createdAt: string
  /** Valor que entrou no destino, na moeda dele (igual a `amount` entre contas da mesma moeda). */
  receivedAmount: number
  /** Nome da conta de origem, mesmo que ela seja de outro espaço (pessoal x grupo): só o nome. */
  fromAccountName: string | null
  /** Nome da conta de destino, nas mesmas condições. */
  toAccountName: string | null
  /** Quem criou a transferência; nulo quando não se sabe. Numa conta compartilhada só essa pessoa exclui. */
  createdByUserId?: number | null
  createdByName?: string | null
}

export interface TransferInput {
  fromAccountId: number
  toAccountId: number
  /** Valor que sai da origem, na moeda dela. */
  amount: number
  transferDate: string
  description?: string
  /** Obrigatório só entre contas de moedas diferentes. */
  receivedAmount?: number
}

/** Conta de outro espaço (pessoal ou grupo) com a qual dá para transferir; sem saldo nem lançamentos. */
export interface LinkableAccount {
  id: number
  name: string
  type: string
  currency: Currency
  /** Nome do espaço onde a conta está. */
  householdName: string
}
