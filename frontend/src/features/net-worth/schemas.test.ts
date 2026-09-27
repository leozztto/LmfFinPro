import { describe, expect, it } from 'vitest'
import { debtBalanceSchema, debtCreateSchema, debtUpdateSchema } from './schemas'

describe('debtCreateSchema', () => {
  it('accepts a debt with its current balance and trims the name', () => {
    const result = debtCreateSchema.parse({
      name: ' Carro ',
      type: 'FINANCING',
      creditor: '',
      balance: '30000',
      balanceDate: '2026-09-27',
    })
    expect(result.name).toBe('Carro')
    expect(result.balance).toBe(30000)
  })

  it('rejects missing name, negative balance and missing date', () => {
    const result = debtCreateSchema.safeParse({ name: '', type: 'LOAN', balance: '-1', balanceDate: '' })
    expect(result.success).toBe(false)
    const fields = result.success ? [] : result.error.issues.map((issue) => issue.path[0])
    expect(fields).toEqual(expect.arrayContaining(['name', 'balance', 'balanceDate']))
  })
})

describe('debtUpdateSchema', () => {
  it('does not ask for a balance', () => {
    expect(debtUpdateSchema.safeParse({ name: 'Cartão', type: 'CREDIT_CARD' }).success).toBe(true)
  })

  it('limits the creditor to 100 characters', () => {
    expect(debtUpdateSchema.safeParse({ name: 'Cartão', type: 'OTHER', creditor: 'x'.repeat(101) }).success).toBe(
      false,
    )
  })
})

describe('debtBalanceSchema', () => {
  it('accepts zero (paid off) but not a negative balance', () => {
    expect(debtBalanceSchema.safeParse({ balance: '0', balanceDate: '2026-09-27' }).success).toBe(true)
    expect(debtBalanceSchema.safeParse({ balance: '-10', balanceDate: '2026-09-27' }).success).toBe(false)
  })
})
