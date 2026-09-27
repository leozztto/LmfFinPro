import { describe, expect, it } from 'vitest'
import { EMPTY_TRANSACTION_REPORT_FILTERS } from './types'
import { buildTransactionReportQuery, validateTransactionReportFilters } from './utils'

describe('buildTransactionReportQuery', () => {
  it('sends only the format when no filter is filled', () => {
    expect(buildTransactionReportQuery(EMPTY_TRANSACTION_REPORT_FILTERS, 'PDF')).toBe('format=PDF')
  })

  it('sends only the filled filters, trimmed and encoded', () => {
    const query = buildTransactionReportQuery(
      { ...EMPTY_TRANSACTION_REPORT_FILTERS, startDate: '2026-09-01', categoryId: '5', description: ' café & cia ' },
      'CSV',
    )

    expect(query).toBe('startDate=2026-09-01&categoryId=5&description=caf%C3%A9+%26+cia&format=CSV')
  })

  it('repeats tagIds so the backend matches any of the tags', () => {
    expect(buildTransactionReportQuery(EMPTY_TRANSACTION_REPORT_FILTERS, 'CSV', [3, 7])).toBe(
      'tagIds=3&tagIds=7&format=CSV',
    )
  })

  it('ignores filters with only spaces', () => {
    expect(buildTransactionReportQuery({ ...EMPTY_TRANSACTION_REPORT_FILTERS, description: '   ' }, 'PDF')).toBe(
      'format=PDF',
    )
  })
})

describe('validateTransactionReportFilters', () => {
  it('accepts empty filters', () => {
    expect(validateTransactionReportFilters(EMPTY_TRANSACTION_REPORT_FILTERS)).toBeNull()
  })

  it('rejects an inverted period', () => {
    expect(
      validateTransactionReportFilters({
        ...EMPTY_TRANSACTION_REPORT_FILTERS,
        startDate: '2026-09-30',
        endDate: '2026-09-01',
      }),
    ).not.toBeNull()
  })

  it('rejects inverted or negative amounts', () => {
    expect(
      validateTransactionReportFilters({ ...EMPTY_TRANSACTION_REPORT_FILTERS, minAmount: '500', maxAmount: '100' }),
    ).not.toBeNull()
    expect(validateTransactionReportFilters({ ...EMPTY_TRANSACTION_REPORT_FILTERS, minAmount: '-1' })).not.toBeNull()
  })
})
