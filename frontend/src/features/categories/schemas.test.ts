import { describe, expect, it } from 'vitest'
import { categorySchema } from './schemas'

describe('categorySchema', () => {
  it('accepts valid input with optional fields omitted', () => {
    const result = categorySchema.safeParse({ name: 'Consultoria', type: 'INCOME' })

    expect(result.success).toBe(true)
  })

  it('accepts valid input with color and icon', () => {
    const result = categorySchema.safeParse({ name: 'Transporte', type: 'EXPENSE', color: '#000', icon: 'car' })

    expect(result.success).toBe(true)
  })

  it('rejects an empty name', () => {
    const result = categorySchema.safeParse({ name: '', type: 'EXPENSE' })

    expect(result.success).toBe(false)
  })

  it('rejects a type outside INCOME/EXPENSE', () => {
    const result = categorySchema.safeParse({ name: 'X', type: 'TRANSFER' })

    expect(result.success).toBe(false)
  })
})
