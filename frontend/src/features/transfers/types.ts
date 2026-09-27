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
