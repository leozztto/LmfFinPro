export interface Transfer {
  id: number
  fromAccountId: number
  toAccountId: number
  amount: number
  transferDate: string
  description: string | null
  fromTransactionId: number
  toTransactionId: number
  createdAt: string
}

export interface TransferInput {
  fromAccountId: number
  toAccountId: number
  amount: number
  transferDate: string
  description?: string
}
