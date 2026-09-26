import { describe, expect, it } from 'vitest'
import { recurringTransactionSchema, recurringTransactionUpdateSchema } from './schemas'

const validInput = {
  accountId: '1',
  categoryId: '',
  clientId: '',
  description: 'Aluguel',
  amount: '1500',
  type: 'EXPENSE',
  frequency: 'MONTHLY',
  startDate: '2026-09-05',
  endDate: '',
}

describe('recurringTransactionSchema', () => {
  it('accepts valid input and turns empty optional fields into undefined', () => {
    const result = recurringTransactionSchema.safeParse(validInput)

    expect(result.success).toBe(true)
    expect(result.data?.categoryId).toBeUndefined()
    expect(result.data?.clientId).toBeUndefined()
    expect(result.data?.endDate).toBeUndefined()
  })

  it('accepts an end date equal to the start date', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, endDate: '2026-09-05' })

    expect(result.success).toBe(true)
  })

  it('rejects an end date before the start date', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, endDate: '2026-09-04' })

    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path).toEqual(['endDate'])
  })

  it('rejects a missing account', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, accountId: '' })

    expect(result.success).toBe(false)
  })

  it('rejects a zero amount', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, amount: '0' })

    expect(result.success).toBe(false)
  })

  it('rejects an empty description', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, description: '' })

    expect(result.success).toBe(false)
  })

  it('rejects an empty start date', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, startDate: '' })

    expect(result.success).toBe(false)
  })

  it('rejects an unknown frequency', () => {
    const result = recurringTransactionSchema.safeParse({ ...validInput, frequency: 'DAILY' })

    expect(result.success).toBe(false)
  })
})

describe('recurringTransactionUpdateSchema', () => {
  const validUpdate = {
    categoryId: '3',
    clientId: '',
    description: 'Aluguel reajustado',
    amount: '1650',
    endDate: '',
    active: false,
  }

  it('accepts valid input', () => {
    const result = recurringTransactionUpdateSchema.safeParse(validUpdate)

    expect(result.success).toBe(true)
    expect(result.data?.categoryId).toBe(3)
    expect(result.data?.active).toBe(false)
  })

  it('rejects a negative amount', () => {
    const result = recurringTransactionUpdateSchema.safeParse({ ...validUpdate, amount: '-1' })

    expect(result.success).toBe(false)
  })

  it('rejects an empty description', () => {
    const result = recurringTransactionUpdateSchema.safeParse({ ...validUpdate, description: '' })

    expect(result.success).toBe(false)
  })
})
