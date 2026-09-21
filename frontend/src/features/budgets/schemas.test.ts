import { describe, expect, it } from 'vitest'
import { budgetSchema } from './schemas'

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
