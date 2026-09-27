import { formatCurrency, type Currency } from '@/shared/format/currency'
import { formatMonthLabel } from '@/features/dashboard/utils'
import type { NetWorthPoint } from './types'

const percentFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
})

/** Valor com sinal explícito: "+ R$ 100,00" / "− R$ 50,00" / "R$ 0,00". */
export function formatSignedCurrency(value: number, currency: Currency = 'BRL'): string {
  if (value === 0) return formatCurrency(0, currency)
  return `${value > 0 ? '+' : '−'} ${formatCurrency(Math.abs(value), currency)}`
}

/** Fração do backend (0.1 = 10%) com sinal; null quando não há base para o percentual. */
export function formatGainRate(rate: number | null): string | null {
  if (rate == null) return null
  const formatted = percentFormatter.format(Math.abs(rate))
  return rate > 0 ? `+${formatted}` : rate < 0 ? `−${formatted}` : formatted
}

export interface NetWorthChartPoint extends NetWorthPoint {
  /** Rótulo curto do mês, ex.: "set/26". */
  label: string
}

export function toChartPoints(points: NetWorthPoint[]): NetWorthChartPoint[] {
  return points.map((point) => ({ ...point, label: formatMonthLabel(point.month) }))
}
