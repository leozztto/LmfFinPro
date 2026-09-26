import { describe, expect, it } from 'vitest'
import { transactionSchema } from './schemas'

describe('transactionSchema', () => {
  it('accepts valid input without category or client', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      categoryId: '',
      clientId: '',
      description: 'Pagamento',
      amount: '100',
      transactionDate: '2026-09-20',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(true)
  })

  it('accepts valid input with category and client', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      categoryId: '5',
      clientId: '3',
      description: 'Pagamento',
      amount: '100',
      transactionDate: '2026-09-20',
      type: 'INCOME',
    })

    expect(result.success).toBe(true)
  })

  it('rejects a missing account', () => {
    const result = transactionSchema.safeParse({
      accountId: '',
      description: 'Pagamento',
      amount: '100',
      transactionDate: '2026-09-20',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(false)
  })

  it('rejects an empty description', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      description: '',
      amount: '100',
      transactionDate: '2026-09-20',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a zero amount', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      description: 'X',
      amount: '0',
      transactionDate: '2026-09-20',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a negative amount', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      description: 'X',
      amount: '-50',
      transactionDate: '2026-09-20',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a missing transaction date', () => {
    const result = transactionSchema.safeParse({
      accountId: '1',
      description: 'X',
      amount: '100',
      transactionDate: '',
      type: 'EXPENSE',
    })

    expect(result.success).toBe(false)
  })

  const baseInput = {
    accountId: '1',
    description: 'Pagamento',
    amount: '100',
    transactionDate: '2026-09-20',
    type: 'EXPENSE',
  }

  it('treats an empty status as automatic (undefined)', () => {
    const result = transactionSchema.safeParse({ ...baseInput, status: '' })

    expect(result.success).toBe(true)
    expect(result.data?.status).toBeUndefined()
  })

  it('accepts an explicit pending status', () => {
    const result = transactionSchema.safeParse({ ...baseInput, status: 'PENDING' })

    expect(result.data?.status).toBe('PENDING')
  })

  it('rejects an unknown status', () => {
    const result = transactionSchema.safeParse({ ...baseInput, status: 'LATE' })

    expect(result.success).toBe(false)
  })
})
