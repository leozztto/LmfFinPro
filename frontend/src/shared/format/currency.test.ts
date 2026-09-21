import { describe, expect, it } from 'vitest'
import { formatCurrency } from './currency'

describe('formatCurrency', () => {
  it('formats positive values as BRL currency', () => {
    expect(formatCurrency(1234.5)).toBe('R$ 1.234,50')
  })

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('R$ 0,00')
  })

  it('formats negative values with a leading minus sign', () => {
    expect(formatCurrency(-50)).toBe('-R$ 50,00')
  })
})
