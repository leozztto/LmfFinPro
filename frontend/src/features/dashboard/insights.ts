import { formatCurrency } from '@/shared/format/currency'
import type { Insight } from './types'

export const INSIGHT_TITLES: Record<Insight['type'], string> = {
  SUBSCRIPTION: 'Assinatura possivelmente esquecida',
  UNUSUAL_EXPENSE: 'Despesa fora do padrão',
  LATE_CLIENT: 'Cliente que costuma atrasar',
}

/** Frase de cada insight, no mesmo tom do e-mail diário. */
export function insightMessage(insight: Insight): string {
  const amount = formatCurrency(insight.amount)
  switch (insight.type) {
    case 'SUBSCRIPTION':
      return `${insight.subject}: cobrado há ${insight.count} meses seguidos (${amount}/mês). Ainda faz sentido manter?`
    case 'UNUSUAL_EXPENSE':
      return `${insight.subject}: ${amount}, bem acima da sua média de ${formatCurrency(insight.reference ?? 0)} nessa categoria.`
    case 'LATE_CLIENT':
      return `${insight.subject} atrasou ${insight.count} recebimentos nos últimos 90 dias, somando ${amount}.`
  }
}
