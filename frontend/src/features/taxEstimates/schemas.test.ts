import { describe, expect, it } from 'vitest'
import { taxEstimateSchema } from './schemas'

describe('taxEstimateSchema', () => {
  it('accepts valid input', () => {
    const result = taxEstimateSchema.safeParse({
      referenceMonth: '2026-09',
      regime: 'MEI',
      grossRevenue: '5000',
      appliedRate: '0.06',
    })

    expect(result.success).toBe(true)
  })

  it('rejects a regime outside the known enum', () => {
    const result = taxEstimateSchema.safeParse({
      referenceMonth: '2026-09',
      regime: 'INVALIDO',
      grossRevenue: '5000',
      appliedRate: '0.06',
    })

    expect(result.success).toBe(false)
  })

  it('rejects zero or negative gross revenue', () => {
    const result = taxEstimateSchema.safeParse({
      referenceMonth: '2026-09',
      regime: 'MEI',
      grossRevenue: '0',
      appliedRate: '0.06',
    })

    expect(result.success).toBe(false)
  })

  it('accepts a zero applied rate (e.g. "OUTRO" regime, preenchido manualmente)', () => {
    const result = taxEstimateSchema.safeParse({
      referenceMonth: '2026-09',
      regime: 'OUTRO',
      grossRevenue: '5000',
      appliedRate: '0',
    })

    expect(result.success).toBe(true)
  })

  it('rejects a negative applied rate', () => {
    const result = taxEstimateSchema.safeParse({
      referenceMonth: '2026-09',
      regime: 'MEI',
      grossRevenue: '5000',
      appliedRate: '-0.1',
    })

    expect(result.success).toBe(false)
  })
})
