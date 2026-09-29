import { describe, expect, it } from 'vitest'
import { budgetSchema, recurringBudgetSchema, recurringBudgetUpdateSchema } from './schemas'

describe('budgetSchema', () => {
  it('accepts valid input', () => {
    const result = budgetSchema.safeParse({ categoryId: '5', referenceMonth: '2026-09', limitValue: '500' })

    expect(result.success).toBe(true)
  })

  it('rejects a missing category', () => {
    const result = budgetSchema.safeParse({ categoryId: '', referenceMonth: '2026-09', limitValue: '500' })

    expect(result.success).toBe(false)
  })

  it('rejects a zero limit value', () => {
    const result = budgetSchema.safeParse({ categoryId: '5', referenceMonth: '2026-09', limitValue: '0' })

    expect(result.success).toBe(false)
  })

  it('rejects a negative limit value', () => {
    const result = budgetSchema.safeParse({ categoryId: '5', referenceMonth: '2026-09', limitValue: '-100' })

    expect(result.success).toBe(false)
  })

  it('rejects an empty reference month', () => {
    const result = budgetSchema.safeParse({ categoryId: '5', referenceMonth: '', limitValue: '500' })

    expect(result.success).toBe(false)
  })
})

describe('recurringBudgetSchema', () => {
  it('accepts valid input without an end month', () => {
    const result = recurringBudgetSchema.safeParse({ categoryId: '5', limitValue: '500', startMonth: '2026-09' })

    expect(result.success).toBe(true)
  })

  it('accepts an end month on or after the start month', () => {
    const result = recurringBudgetSchema.safeParse({
      categoryId: '5',
      limitValue: '500',
      startMonth: '2026-09',
      endMonth: '2026-12',
    })

    expect(result.success).toBe(true)
  })

  it('rejects an end month before the start month', () => {
    const result = recurringBudgetSchema.safeParse({
      categoryId: '5',
      limitValue: '500',
      startMonth: '2026-09',
      endMonth: '2026-08',
    })

    expect(result.success).toBe(false)
  })

  it('rejects a zero limit value', () => {
    const result = recurringBudgetSchema.safeParse({ categoryId: '5', limitValue: '0', startMonth: '2026-09' })

    expect(result.success).toBe(false)
  })

  it('rejects a missing start month', () => {
    const result = recurringBudgetSchema.safeParse({ categoryId: '5', limitValue: '500', startMonth: '' })

    expect(result.success).toBe(false)
  })
})

describe('recurringBudgetUpdateSchema', () => {
  it('accepts valid input', () => {
    const result = recurringBudgetUpdateSchema('2026-09').safeParse({ limitValue: '500', active: true })

    expect(result.success).toBe(true)
  })

  it('rejects a zero limit value', () => {
    const result = recurringBudgetUpdateSchema('2026-09').safeParse({ limitValue: '0', active: true })

    expect(result.success).toBe(false)
  })

  it('accepts an end month on or after the fixed start month', () => {
    const result = recurringBudgetUpdateSchema('2026-09').safeParse({
      limitValue: '500',
      endMonth: '2026-09',
      active: true,
    })

    expect(result.success).toBe(true)
  })

  it('rejects an end month before the fixed start month', () => {
    const result = recurringBudgetUpdateSchema('2026-09').safeParse({
      limitValue: '500',
      endMonth: '2026-08',
      active: true,
    })

    expect(result.success).toBe(false)
  })
})
