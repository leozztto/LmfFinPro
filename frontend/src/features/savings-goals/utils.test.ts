import { describe, expect, it } from 'vitest'
import { formatPercent, goalProgress, percentToRate, rateToPercent } from './utils'

describe('goalProgress', () => {
  it('returns the percentage saved, capped between 0 and 100', () => {
    expect(goalProgress(250, 1000)).toBe(25)
    expect(goalProgress(1500, 1000)).toBe(100)
    expect(goalProgress(-10, 1000)).toBe(0)
    expect(goalProgress(100, 0)).toBe(0)
  })
})

describe('rate conversion', () => {
  it('converts between backend fraction and screen percent without float noise', () => {
    expect(rateToPercent(0.06)).toBe(6)
    expect(rateToPercent(0.1133)).toBe(11.33)
    expect(percentToRate(6)).toBe(0.06)
    expect(percentToRate(27.5)).toBe(0.275)
  })

  it('formats the rate as a pt-BR percent', () => {
    expect(formatPercent(0.1133)).toBe('11,33%')
    expect(formatPercent(0.06)).toBe('6%')
  })
})
