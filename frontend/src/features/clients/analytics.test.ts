import { describe, expect, it } from 'vitest'
import { NEUTRAL_SERIES_COLOR } from '@/shared/chart/palette'
import { OTHERS_KEY, assignClientColors, buildStackedHistory, describeRisk, formatShare } from './analytics'
import type { ClientRankingRow } from './types'

const MONTHS = ['2026-08', '2026-09']

function row(clientId: number, income: number[], color: string | null = null): ClientRankingRow {
  const total = income.reduce((sum, value) => sum + value, 0)
  return {
    clientId,
    name: `Cliente ${clientId}`,
    color,
    income: total,
    expense: 0,
    net: total,
    incomeCount: income.filter((value) => value > 0).length,
    averageTicket: 0,
    share: 0,
    activeMonths: 0,
    lastIncomeDate: null,
    monthly: MONTHS.map((month, index) => ({ month, income: income[index], expense: 0 })),
  }
}

describe('assignClientColors', () => {
  it('keeps the client own color and gives the palette only to clients without one', () => {
    const colors = assignClientColors([row(1, [1, 1], '#123456'), row(2, [1, 0]), row(3, [0, 1])], ['#aaa', '#bbb'])

    expect(colors.get(1)).toBe('#123456')
    expect(colors.get(2)).toBe('#aaa')
    expect(colors.get(3)).toBe('#bbb')
  })
})

describe('buildStackedHistory', () => {
  it('keeps the top five clients and folds the rest into Outros', () => {
    const ranking = [1, 2, 3, 4, 5, 6, 7].map((id) => row(id, [100 - id, 10]))
    const { data, series } = buildStackedHistory(MONTHS, ranking, assignClientColors(ranking, ['#aaa']))

    expect(series.map((serie) => serie.key)).toEqual([
      'client-1',
      'client-2',
      'client-3',
      'client-4',
      'client-5',
      OTHERS_KEY,
    ])
    expect(series[5]).toMatchObject({ name: 'Outros (2)', color: NEUTRAL_SERIES_COLOR })
    expect(data[0]).toMatchObject({ month: '2026-08', label: 'ago/26', 'client-1': 99, [OTHERS_KEY]: 94 + 93 })
  })

  it('leaves out clients with expenses only', () => {
    const ranking = [row(1, [100, 0]), { ...row(2, [0, 0]), expense: 50 }]
    const { series } = buildStackedHistory(MONTHS, ranking, assignClientColors(ranking, ['#aaa']))

    expect(series.map((serie) => serie.key)).toEqual(['client-1'])
  })
})

describe('describeRisk', () => {
  it('names the top client and the share for each level', () => {
    expect(describeRisk('HIGH', 0.7, 'Acme')).toMatchObject({ tone: 'critical' })
    expect(describeRisk('HIGH', 0.7, 'Acme').description).toContain('70% da sua receita vem de Acme')
    expect(describeRisk('MODERATE', 0.4).tone).toBe('warning')
    expect(describeRisk('LOW', 0.2).tone).toBe('good')
    expect(describeRisk('NONE', 0).tone).toBe('neutral')
  })

  it('does not call the income diversified when much of it has no client', () => {
    const info = describeRisk('LOW', 0.255, 'Acme', 0.745)

    expect(info.label).toBe('Análise incompleta')
    expect(info.tone).toBe('warning')
    expect(info.description).toContain('74,5% da receita do período está sem cliente')
    expect(describeRisk('HIGH', 0.6, 'Acme', 0.35).description).toContain('35% da receita está sem cliente')
    expect(describeRisk('LOW', 0.2, 'Acme', 0.1).label).toBe('Receita diversificada')
  })
})

describe('formatShare', () => {
  it('formats a fraction as a pt-BR percent with one decimal', () => {
    expect(formatShare(0.8235)).toBe('82,4%')
    expect(formatShare(0.5)).toBe('50%')
  })
})
