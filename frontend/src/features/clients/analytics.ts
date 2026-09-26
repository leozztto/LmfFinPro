import { NEUTRAL_SERIES_COLOR } from '@/shared/chart/palette'
import { formatMonthLabel } from '@/features/dashboard/utils'
import type { ClientRankingRow, ConcentrationRisk } from './types'

/** Quantos clientes aparecem com cor própria no gráfico empilhado; o resto vira "Outros". */
export const STACKED_TOP_CLIENTS = 5

export const OTHERS_KEY = 'others'

export interface StackedSeries {
  key: string
  name: string
  color: string
}

/**
 * Cor de cada cliente: a cadastrada no cliente (a mesma do resto do app) ou, sem ela, uma da paleta
 * categórica na ordem do ranking. Clientes com cor própria não consomem posições da paleta.
 */
export function assignClientColors(ranking: ClientRankingRow[], palette: string[]): Map<number, string> {
  const colors = new Map<number, string>()
  let paletteIndex = 0
  for (const row of ranking) {
    if (row.color) {
      colors.set(row.clientId, row.color)
    } else {
      colors.set(row.clientId, palette[Math.min(paletteIndex, palette.length - 1)])
      paletteIndex++
    }
  }
  return colors
}

/**
 * Dados do gráfico empilhado: uma linha por mês, uma coluna por cliente do top 5 (com receita no
 * período) e "Outros" somando os demais.
 */
export function buildStackedHistory(
  months: string[],
  ranking: ClientRankingRow[],
  colors: Map<number, string>,
): { data: Array<Record<string, number | string>>; series: StackedSeries[] } {
  const withIncome = ranking.filter((row) => row.income > 0)
  const top = withIncome.slice(0, STACKED_TOP_CLIENTS)
  const others = withIncome.slice(STACKED_TOP_CLIENTS)

  const series: StackedSeries[] = top.map((row) => ({
    key: `client-${row.clientId}`,
    name: row.name,
    color: colors.get(row.clientId) ?? NEUTRAL_SERIES_COLOR,
  }))
  if (others.length > 0) {
    series.push({ key: OTHERS_KEY, name: `Outros (${others.length})`, color: NEUTRAL_SERIES_COLOR })
  }

  const data = months.map((month, index) => {
    const point: Record<string, number | string> = { month, label: formatMonthLabel(month) }
    for (const row of top) {
      point[`client-${row.clientId}`] = row.monthly[index]?.income ?? 0
    }
    if (others.length > 0) {
      point[OTHERS_KEY] = others.reduce((sum, row) => sum + (row.monthly[index]?.income ?? 0), 0)
    }
    return point
  })
  return { data, series }
}

export interface RiskInfo {
  label: string
  description: string
  tone: 'good' | 'warning' | 'critical' | 'neutral'
}

/** A partir dessa fração de receita sem cliente, a análise de concentração não é conclusiva. */
export const UNASSIGNED_WARNING_SHARE = 0.3

/**
 * Faixas iguais às do backend (ConcentrationRisk): 30% e 50% da receita em um só cliente. Com muita
 * receita sem cliente vinculado, "diversificada" seria enganoso — o dinheiro sem cliente pode vir
 * todo de um só pagador —, então a mensagem avisa que a análise está incompleta.
 */
export function describeRisk(
  risk: ConcentrationRisk,
  topShare: number,
  topClientName?: string,
  unassignedShare = 0,
): RiskInfo {
  const percent = formatShare(topShare)
  const who = topClientName ?? 'um único cliente'
  const hasManyUnassigned = unassignedShare >= UNASSIGNED_WARNING_SHARE
  const unassignedNote = hasManyUnassigned
    ? ` Além disso, ${formatShare(unassignedShare)} da receita está sem cliente vinculado.`
    : ''
  switch (risk) {
    case 'HIGH':
      return {
        label: 'Concentração alta',
        description: `${percent} da sua receita vem de ${who}. Se esse cliente parar, a maior parte do faturamento vai junto.${unassignedNote}`,
        tone: 'critical',
      }
    case 'MODERATE':
      return {
        label: 'Concentração moderada',
        description: `${percent} da sua receita vem de ${who}. Vale diversificar para reduzir a dependência.${unassignedNote}`,
        tone: 'warning',
      }
    case 'LOW':
      if (hasManyUnassigned) {
        return {
          label: 'Análise incompleta',
          description: `${formatShare(unassignedShare)} da receita do período está sem cliente vinculado, então não dá para afirmar que ela é diversificada. Vincule o cliente nos lançamentos para ver a concentração real.`,
          tone: 'warning',
        }
      }
      return {
        label: 'Receita diversificada',
        description: `O maior cliente (${who}) responde por ${percent} da receita.`,
        tone: 'good',
      }
    default:
      return {
        label: 'Sem receita no período',
        description: 'Não há receitas no período escolhido para medir a concentração.',
        tone: 'neutral',
      }
  }
}

/** Fração (0.8235) → "82,4%". */
export function formatShare(share: number): string {
  return `${(share * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`
}
