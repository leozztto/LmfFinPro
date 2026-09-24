/**
 * Escala de cor da barra (HSL), por faixa de gasto em relação ao limite:
 * - 0% → 25%: azul claro → verde
 * - 25% → 100%: verde → amarelo → laranja-avermelhado
 * - acima de 100%: vermelho (OVER_BUDGET_COLOR)
 */
const COLOR_STOPS = [
  { ratio: 0, hue: 200, lightness: 60 },
  { ratio: 0.25, hue: 140, lightness: 45 },
  { ratio: 1, hue: 15, lightness: 45 },
]
const SATURATION = 72
/** Vermelho reservado para quando o limite é ultrapassado. */
export const OVER_BUDGET_COLOR = 'hsl(0 72% 51%)'

export interface BudgetUsage {
  /** Largura da barra, de 0 a 100 (trava em 100 quando ultrapassa). */
  percentage: number
  isOverBudget: boolean
  /** Cor da barra conforme a faixa de gasto (ver COLOR_STOPS). */
  color: string
}

function interpolateColor(ratio: number): string {
  const endIndex = COLOR_STOPS.findIndex((stop) => ratio <= stop.ratio)
  const end = COLOR_STOPS[Math.max(endIndex, 1)]
  const start = COLOR_STOPS[Math.max(endIndex, 1) - 1]
  const progress = (ratio - start.ratio) / (end.ratio - start.ratio)
  const hue = Math.round(start.hue + (end.hue - start.hue) * progress)
  const lightness = Math.round(start.lightness + (end.lightness - start.lightness) * progress)
  return `hsl(${hue} ${SATURATION}% ${lightness}%)`
}

export function budgetUsage(spent: number, limit: number): BudgetUsage {
  const ratio = limit > 0 ? Math.max(0, spent / limit) : spent > 0 ? Infinity : 0
  const isOverBudget = ratio > 1
  if (isOverBudget) {
    return { percentage: 100, isOverBudget, color: OVER_BUDGET_COLOR }
  }
  return { percentage: ratio * 100, isOverBudget, color: interpolateColor(ratio) }
}
