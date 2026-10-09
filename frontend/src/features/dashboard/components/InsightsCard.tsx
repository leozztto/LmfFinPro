import { Link } from 'react-router-dom'
import { BellIcon } from '@/shared/ui/icons'
import { useInsights } from '../hooks/useInsights'
import { INSIGHT_TITLES, insightMessage } from '../insights'

/**
 * Insights automáticos do grupo ativo (os mesmos do e-mail diário, sem o filtro de "já avisado").
 * Some quando não há nada a mostrar, para não ocupar a Visão geral à toa.
 */
export function InsightsCard() {
  const { data } = useInsights()
  if (!data || data.length === 0) return null

  return (
    <section
      aria-label="Insights automáticos"
      className="space-y-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200 sm:p-4"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 font-semibold">
          <BellIcon className="h-4 w-4 shrink-0" />
          Insights
        </h3>
        <Link to="/configuracoes/notificacoes" className="text-xs font-medium underline">
          Ajustar avisos
        </Link>
      </div>
      <ul className="space-y-2">
        {data.map((insight) => (
          <li key={`${insight.type}-${insight.subject}`} className="min-w-0">
            <span className="block text-xs font-medium uppercase tracking-wide opacity-80">
              {INSIGHT_TITLES[insight.type]}
            </span>
            <span className="block break-words">{insightMessage(insight)}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
