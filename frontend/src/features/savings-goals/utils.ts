import type { SavingsGoalType } from './types'

export const GOAL_TYPE_LABELS: Record<SavingsGoalType, string> = {
  EMERGENCY_FUND: 'Reserva de emergência',
  TAX_RESERVE: 'Caixinha do imposto',
  VACATION: 'Férias',
  OTHER: 'Outra meta',
}

/** Percentual atingido, limitado a 100 para a barra de progresso. */
export function goalProgress(saved: number, target: number): number {
  if (target <= 0) return 0
  return Math.min(100, Math.max(0, (saved / target) * 100))
}

/** Fração do backend (0.06) → percentual da tela (6), sem ruído de ponto flutuante. */
export function rateToPercent(rate: number): number {
  return Math.round(rate * 10000) / 100
}

/** Percentual da tela (6) → fração do backend (0.06). */
export function percentToRate(percent: number): number {
  return Math.round(percent * 100) / 10000
}

export function formatPercent(rate: number): string {
  return `${rateToPercent(rate).toLocaleString('pt-BR')}%`
}
