import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatDateOnlyBr, getCurrentIsoDate, getCurrentYearMonth } from './date'

describe('formatDateOnlyBr', () => {
  it('converts an ISO date to DD/MM/YYYY without timezone shifting', () => {
    expect(formatDateOnlyBr('2026-09-20')).toBe('20/09/2026')
  })

  it('pads nothing since the input is already zero-padded', () => {
    expect(formatDateOnlyBr('2026-01-05')).toBe('05/01/2026')
  })
})

describe('getCurrentIsoDate / getCurrentYearMonth', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns the local date as YYYY-MM-DD', () => {
    vi.setSystemTime(new Date(2026, 8, 5)) // 5 de setembro de 2026 (mês 0-indexado)

    expect(getCurrentIsoDate()).toBe('2026-09-05')
  })

  it('pads single-digit month and day', () => {
    vi.setSystemTime(new Date(2026, 0, 3)) // 3 de janeiro de 2026

    expect(getCurrentIsoDate()).toBe('2026-01-03')
  })

  it('returns the local year-month as YYYY-MM', () => {
    vi.setSystemTime(new Date(2026, 8, 20))

    expect(getCurrentYearMonth()).toBe('2026-09')
  })
})
