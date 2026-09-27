import { describe, expect, it } from 'vitest'
import { formatGainRate, formatSignedCurrency, toChartPoints } from './utils'

const normalize = (value: string | null) => value?.replace(/\s/g, ' ')

describe('formatSignedCurrency', () => {
  it('shows the sign of gains and losses', () => {
    expect(normalize(formatSignedCurrency(100))).toBe('+ R$ 100,00')
    expect(normalize(formatSignedCurrency(-50.5))).toBe('− R$ 50,50')
    expect(normalize(formatSignedCurrency(0))).toBe('R$ 0,00')
  })
})

describe('formatGainRate', () => {
  it('formats the fraction as a signed percentage', () => {
    expect(normalize(formatGainRate(0.1))).toBe('+10,0%')
    expect(normalize(formatGainRate(-0.0525))).toBe('−5,25%')
    expect(normalize(formatGainRate(0))).toBe('0,0%')
  })

  it('returns null without a base', () => {
    expect(formatGainRate(null)).toBeNull()
  })
})

describe('toChartPoints', () => {
  it('adds the short month label', () => {
    const [point] = toChartPoints([{ month: '2026-09', cash: 1, investments: 2, debts: 3, netWorth: 0 }])
    expect(point.label).toBe('set/26')
    expect(point.netWorth).toBe(0)
  })
})
