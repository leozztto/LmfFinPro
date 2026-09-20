import type { Category } from '@/features/categories/types'
import type { Client } from '@/features/clients/types'
import type { RawBalancePoint, RawBreakdownPoint, RawCashFlowProjectionPoint, RawMonthlyFlowPoint } from './types'

const MONTH_LABELS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

const UNASSIGNED_COLOR = '#94a3b8'

export interface MonthlyFlowPoint {
  month: string
  label: string
  income: number
  expense: number
}

/** Ponto de um gráfico de barras "total por entidade" (categoria, cliente, etc.). */
export interface BreakdownPoint {
  id: number | null
  name: string
  color: string
  value: number
}

export interface BalancePoint {
  month: string
  label: string
  balance: number
}

export interface CashFlowProjectionPoint extends BalancePoint {
  /** true para meses futuros (projetados); false para o histórico real. */
  isProjected: boolean
}

export function formatMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-')
  return `${MONTH_LABELS[Number(month) - 1]}/${year.slice(2)}`
}

/** Anexa o rótulo de exibição (ex: "set/26") ao mês bruto vindo da API. */
export function toMonthlyFlowPoints(points: RawMonthlyFlowPoint[]): MonthlyFlowPoint[] {
  return points.map((point) => ({ ...point, label: formatMonthLabel(point.month) }))
}

export function toBalancePoints(points: RawBalancePoint[]): BalancePoint[] {
  return points.map((point) => ({ ...point, label: formatMonthLabel(point.month) }))
}

export function toCashFlowProjectionPoints(points: RawCashFlowProjectionPoint[]): CashFlowProjectionPoint[] {
  return points.map((point) => ({
    month: point.month,
    label: formatMonthLabel(point.month),
    balance: point.balance,
    isProjected: point.projected,
  }))
}

/** Resolve nome/cor de cada entrada a partir do cache de categorias já carregado. */
export function toCategoryBreakdownPoints(points: RawBreakdownPoint[], categories: Category[]): BreakdownPoint[] {
  const categoryById = new Map(categories.map((category) => [category.id, category]))
  return points.map((point) => {
    const category = point.entityId != null ? categoryById.get(point.entityId) : undefined
    return {
      id: point.entityId,
      name: category?.name ?? 'Sem categoria',
      color: category?.color ?? UNASSIGNED_COLOR,
      value: point.value,
    }
  })
}

/** Resolve nome/cor de cada entrada a partir do cache de clientes já carregado. */
export function toClientBreakdownPoints(points: RawBreakdownPoint[], clients: Client[]): BreakdownPoint[] {
  const clientById = new Map(clients.map((client) => [client.id, client]))
  return points.map((point) => {
    const client = point.entityId != null ? clientById.get(point.entityId) : undefined
    return {
      id: point.entityId,
      name: client?.name ?? 'Sem cliente',
      color: client?.color ?? UNASSIGNED_COLOR,
      value: point.value,
    }
  })
}
