import { describe, expect, it } from 'vitest'
import { OVER_BUDGET_COLOR, budgetUsage } from './utils'

const hueOf = (color: string) => Number(color.match(/hsl\((\d+)/)?.[1])

describe('budgetUsage', () => {
  it('starts light blue with an empty bar when nothing was spent', () => {
    expect(budgetUsage(0, 500)).toEqual({ percentage: 0, isOverBudget: false, color: 'hsl(200 72% 60%)' })
  })

  it('reaches green at 25% of the limit', () => {
    const usage = budgetUsage(125, 500)
    expect(usage.percentage).toBe(25)
    expect(usage.color).toBe('hsl(140 72% 45%)')
  })

  it('moves from blue towards green inside the first quarter', () => {
    expect(budgetUsage(62.5, 500).color).toBe('hsl(170 72% 53%)')
  })

  it('keeps shifting from green to yellow after 25%', () => {
    const hue = hueOf(budgetUsage(250, 500).color)
    expect(hue).toBeLessThan(140)
    expect(hue).toBeGreaterThan(60)
  })

  it('reaches orange-red, but not the over-budget red, exactly at the limit', () => {
    const usage = budgetUsage(500, 500)
    expect(usage.percentage).toBe(100)
    expect(usage.isOverBudget).toBe(false)
    expect(usage.color).toBe('hsl(15 72% 45%)')
  })

  it('turns red and caps the bar when the limit is exceeded', () => {
    expect(budgetUsage(501, 500)).toEqual({ percentage: 100, isOverBudget: true, color: OVER_BUDGET_COLOR })
  })

  it('gets warmer as spending grows', () => {
    const hue = (spent: number) => hueOf(budgetUsage(spent, 100).color)
    expect(hue(10)).toBeGreaterThan(hue(25))
    expect(hue(25)).toBeGreaterThan(hue(60))
    expect(hue(60)).toBeGreaterThan(hue(95))
  })

  it('treats any spending against a zero limit as over budget', () => {
    expect(budgetUsage(10, 0).isOverBudget).toBe(true)
    expect(budgetUsage(0, 0)).toEqual({ percentage: 0, isOverBudget: false, color: 'hsl(200 72% 60%)' })
  })
})
