import { describe, expect, it } from 'vitest'
import type { Category } from '@/features/categories/types'
import type { Client } from '@/features/clients/types'
import {
  formatMonthLabel,
  toBalancePoints,
  toCashFlowProjectionPoints,
  toCategoryBreakdownPoints,
  toClientBreakdownPoints,
  toMonthlyFlowPoints,
} from './utils'

describe('formatMonthLabel', () => {
  it('formats a year-month as "mmm/yy" in Portuguese', () => {
    expect(formatMonthLabel('2026-09')).toBe('set/26')
  })

  it('formats January correctly (index 0)', () => {
    expect(formatMonthLabel('2026-01')).toBe('jan/26')
  })

  it('formats December correctly (index 11)', () => {
    expect(formatMonthLabel('2026-12')).toBe('dez/26')
  })
})

describe('toMonthlyFlowPoints', () => {
  it('attaches a display label to each raw point', () => {
    const result = toMonthlyFlowPoints([{ month: '2026-09', income: 500, expense: 200 }])

    expect(result).toEqual([{ month: '2026-09', income: 500, expense: 200, label: 'set/26' }])
  })
})

describe('toBalancePoints', () => {
  it('attaches a display label to each raw point', () => {
    const result = toBalancePoints([{ month: '2026-09', balance: 1500 }])

    expect(result).toEqual([{ month: '2026-09', balance: 1500, label: 'set/26' }])
  })
})

describe('toCashFlowProjectionPoints', () => {
  it('maps the raw "projected" flag to "isProjected"', () => {
    const result = toCashFlowProjectionPoints([{ month: '2026-10', balance: 1900, projected: true }])

    expect(result).toEqual([{ month: '2026-10', label: 'out/26', balance: 1900, isProjected: true }])
  })
})

describe('toCategoryBreakdownPoints', () => {
  const categories: Category[] = [
    { id: 1, name: 'Alimentação', type: 'EXPENSE', color: '#e11d48', icon: null, global: false },
  ]

  it('resolves the category name and color from the cache', () => {
    const result = toCategoryBreakdownPoints([{ entityId: 1, value: 350 }], categories)

    expect(result).toEqual([{ id: 1, name: 'Alimentação', color: '#e11d48', value: 350 }])
  })

  it('falls back to "Sem categoria" when entityId is null', () => {
    const result = toCategoryBreakdownPoints([{ entityId: null, value: 30 }], categories)

    expect(result[0].name).toBe('Sem categoria')
    expect(result[0].color).toBe('#94a3b8')
  })

  it('falls back to "Sem categoria" when the category is not in the cache (e.g. deleted)', () => {
    const result = toCategoryBreakdownPoints([{ entityId: 999, value: 30 }], categories)

    expect(result[0].name).toBe('Sem categoria')
  })
})

describe('toClientBreakdownPoints', () => {
  const clients: Client[] = [
    {
      id: 10,
      name: 'Empresa X',
      email: null,
      phone: null,
      documentType: null,
      documentNumber: null,
      workType: null,
      notes: null,
      color: '#2a78d6',
      active: true,
    },
  ]

  it('resolves the client name and color from the cache', () => {
    const result = toClientBreakdownPoints([{ entityId: 10, value: 1200 }], clients)

    expect(result).toEqual([{ id: 10, name: 'Empresa X', color: '#2a78d6', value: 1200 }])
  })

  it('falls back to "Sem cliente" when entityId is null', () => {
    const result = toClientBreakdownPoints([{ entityId: null, value: 300 }], clients)

    expect(result[0].name).toBe('Sem cliente')
  })
})
