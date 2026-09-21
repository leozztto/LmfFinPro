import { describe, expect, it } from 'vitest'
import { accountSchema } from './schemas'

describe('accountSchema', () => {
  it('accepts valid input', () => {
    const result = accountSchema.safeParse({ name: 'Carteira', type: 'WALLET', initialBalance: '1000' })

    expect(result.success).toBe(true)
  })

  it('accepts a negative initial balance (e.g. a card with a running debt)', () => {
    const result = accountSchema.safeParse({ name: 'Cartão', type: 'CHECKING', initialBalance: '-500' })

    expect(result.success).toBe(true)
  })

  it('rejects an empty name', () => {
    const result = accountSchema.safeParse({ name: '', type: 'WALLET', initialBalance: '0' })

    expect(result.success).toBe(false)
  })

  it('rejects a type outside the known enum', () => {
    const result = accountSchema.safeParse({ name: 'Carteira', type: 'CRYPTO', initialBalance: '0' })

    expect(result.success).toBe(false)
  })

  it('rejects a non-numeric initial balance', () => {
    const result = accountSchema.safeParse({ name: 'Carteira', type: 'WALLET', initialBalance: 'abc' })

    expect(result.success).toBe(false)
  })
})
