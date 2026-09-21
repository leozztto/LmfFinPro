import { describe, expect, it } from 'vitest'
import { transferSchema } from './schemas'

describe('transferSchema', () => {
  it('accepts valid input', () => {
    const result = transferSchema.safeParse({
      fromAccountId: '1',
      toAccountId: '2',
      amount: '200',
      transferDate: '2026-09-20',
      description: '',
    })

    expect(result.success).toBe(true)
  })

  it('rejects when the source and destination accounts are the same', () => {
    const result = transferSchema.safeParse({
      fromAccountId: '1',
      toAccountId: '1',
      amount: '200',
      transferDate: '2026-09-20',
    })

    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0].path).toEqual(['toAccountId'])
    }
  })

  it('rejects a missing source account', () => {
    const result = transferSchema.safeParse({
      fromAccountId: '',
      toAccountId: '2',
      amount: '200',
      transferDate: '2026-09-20',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a zero amount', () => {
    const result = transferSchema.safeParse({
      fromAccountId: '1',
      toAccountId: '2',
      amount: '0',
      transferDate: '2026-09-20',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a missing transfer date', () => {
    const result = transferSchema.safeParse({
      fromAccountId: '1',
      toAccountId: '2',
      amount: '200',
      transferDate: '',
    })

    expect(result.success).toBe(false)
  })
})
