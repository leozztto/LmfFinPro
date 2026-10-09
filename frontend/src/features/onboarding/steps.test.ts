import { describe, expect, it } from 'vitest'
import { ONBOARDING_PARAM, ONBOARDING_PATH, SAMPLE_CSV, TOUR_STEPS } from './steps'

describe('onboarding steps', () => {
  it('has unique steps, each with a title, an explanation and a place to find it', () => {
    const ids = TOUR_STEPS.map((step) => step.id)

    expect(new Set(ids).size).toBe(ids.length)
    for (const step of TOUR_STEPS) {
      expect(step.group).not.toBe('')
      expect(step.title).not.toBe('')
      expect(step.description).not.toBe('')
      expect(step.where).not.toBe('')
    }
  })

  it('keeps the steps of the same task together, in order', () => {
    const groups = TOUR_STEPS.map((step) => step.group)
    const collapsed = groups.filter((group, index) => group !== groups[index - 1])

    expect(new Set(collapsed).size).toBe(collapsed.length)
  })

  it('covers account, transactions, import, budget, recurrences, calendar, dashboard and reports', () => {
    const ids = TOUR_STEPS.map((step) => step.id)

    expect(ids).toEqual(
      expect.arrayContaining([
        'ACCOUNT_SAVE',
        'TRANSACTION_SAVE',
        'IMPORT_RESULT',
        'BUDGET_SAVE',
        'RECURRING_SAVE',
        'CALENDAR_VIEW',
        'DASHBOARD_VIEW',
        'REPORTS_VIEW',
      ]),
    )
    expect(TOUR_STEPS).toHaveLength(14)
  })

  it('opens the guide through the query parameter', () => {
    expect(ONBOARDING_PATH).toBe(`/?${ONBOARDING_PARAM}=1`)
  })

  it('offers a CSV model with a header and rows of income and expense', () => {
    const [header, ...rows] = SAMPLE_CSV.trim().split('\n')

    expect(header).toBe('date,description,amount')
    expect(rows.length).toBeGreaterThan(1)
    expect(rows.some((row) => row.endsWith('-24.90'))).toBe(true)
    expect(rows.some((row) => !row.split(',')[2].startsWith('-'))).toBe(true)
  })
})
