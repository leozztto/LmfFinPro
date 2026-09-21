import { describe, expect, it } from 'vitest'
import { categoryRuleSchema } from './schemas'

describe('categoryRuleSchema', () => {
  it('accepts valid input', () => {
    const result = categoryRuleSchema.safeParse({ pattern: 'UBER', categoryId: '5' })

    expect(result.success).toBe(true)
  })

  it('rejects an empty pattern', () => {
    const result = categoryRuleSchema.safeParse({ pattern: '', categoryId: '5' })

    expect(result.success).toBe(false)
  })

  it('rejects a missing category', () => {
    const result = categoryRuleSchema.safeParse({ pattern: 'UBER', categoryId: '' })

    expect(result.success).toBe(false)
  })
})
