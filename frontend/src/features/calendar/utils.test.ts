import { describe, expect, it } from 'vitest'
import { buildMonthGrid, entryStatusLabel, entryTitle, formatDayLabel, formatMonthLabel, shiftMonth } from './utils'
import type { CalendarEntry } from './types'

function entry(overrides: Partial<CalendarEntry>): CalendarEntry {
  return {
    kind: 'TRANSACTION',
    date: '2026-09-20',
    description: 'Boleto',
    amount: 100,
    type: 'EXPENSE',
    status: 'PENDING',
    transactionId: 1,
    recurringTransactionId: null,
    accountName: 'Conta',
    categoryName: null,
    clientName: null,
    competence: null,
    ...overrides,
  }
}

describe('shiftMonth', () => {
  it('moves forward and backward across year boundaries', () => {
    expect(shiftMonth('2026-09', 1)).toBe('2026-10')
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(shiftMonth('2026-03', -15)).toBe('2024-12')
  })
})

describe('formatMonthLabel', () => {
  it('writes the month name in Portuguese', () => {
    expect(formatMonthLabel('2026-09')).toBe('Setembro de 2026')
  })
})

describe('buildMonthGrid', () => {
  it('starts on Sunday and pads the first and last weeks', () => {
    // 01/09/2026 é uma terça-feira; 30/09 é uma quarta.
    const weeks = buildMonthGrid('2026-09')
    expect(weeks).toHaveLength(5)
    expect(weeks[0]).toEqual([null, null, '2026-09-01', '2026-09-02', '2026-09-03', '2026-09-04', '2026-09-05'])
    expect(weeks[4]).toEqual(['2026-09-27', '2026-09-28', '2026-09-29', '2026-09-30', null, null, null])
  })

  it('handles leap-year February', () => {
    const days = buildMonthGrid('2028-02').flat().filter(Boolean)
    expect(days).toHaveLength(29)
    expect(days.at(-1)).toBe('2028-02-29')
  })
})

describe('entryStatusLabel', () => {
  it('describes each status from the payer or receiver point of view', () => {
    expect(entryStatusLabel(entry({ status: 'PENDING' }))).toBe('A pagar')
    expect(entryStatusLabel(entry({ status: 'PENDING', type: 'INCOME' }))).toBe('A receber')
    expect(entryStatusLabel(entry({ status: 'PAID' }))).toBe('Paga')
    expect(entryStatusLabel(entry({ status: 'PAID', type: 'INCOME' }))).toBe('Recebida')
    expect(entryStatusLabel(entry({ status: 'OVERDUE' }))).toBe('Atrasada')
    expect(entryStatusLabel(entry({ status: 'FORECAST', kind: 'RECURRING_FORECAST' }))).toBe('Prevista')
    expect(entryStatusLabel(entry({ status: 'FORECAST', kind: 'DAS' }))).toBe('Lembrete')
  })
})

describe('entryTitle', () => {
  it('adds the competence to the DAS and keeps other descriptions', () => {
    expect(entryTitle(entry({ kind: 'DAS', description: 'DAS', competence: '2026-08' }))).toBe(
      'DAS · competência 08/2026',
    )
    expect(entryTitle(entry({}))).toBe('Boleto')
  })
})

describe('formatDayLabel', () => {
  it('writes weekday, day and month without timezone shifting', () => {
    expect(formatDayLabel('2026-09-20')).toBe('Domingo, 20 de setembro')
    expect(formatDayLabel('2026-03-01')).toBe('Domingo, 1 de março')
  })
})
