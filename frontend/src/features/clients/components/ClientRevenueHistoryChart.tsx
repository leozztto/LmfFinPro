import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { Card } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import { OTHERS_KEY, type StackedSeries } from '../analytics'

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

interface ClientRevenueHistoryChartProps {
  data: Array<Record<string, number | string>>
  series: StackedSeries[]
}

/**
 * Receita mensal empilhada dos maiores clientes + "Outros". Uma cor por cliente (a mesma da tabela
 * de ranking), legenda sempre visível e a tabela abaixo com os valores — a cor nunca é a única
 * forma de identificar o cliente.
 */
export function ClientRevenueHistoryChart({ data, series }: ClientRevenueHistoryChartProps) {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const gridColor = isDark ? '#3f3f46' : '#e4e4e7'
  const axisColor = isDark ? '#a1a1aa' : '#71717a'
  // Espaço de 2px na cor do fundo do card entre os segmentos empilhados.
  const surfaceColor = isDark ? '#27272a' : '#fafafa'

  return (
    <Card padding="sm">
      <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Receita por cliente, mês a mês</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">{describeSeries(series)}</p>
      {series.length === 0 ? (
        <p className="mt-6 text-sm text-zinc-500 dark:text-zinc-400">Nenhuma receita com cliente no período.</p>
      ) : (
        <>
          <div className="mt-3 h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                <CartesianGrid vertical={false} stroke={gridColor} />
                <XAxis
                  dataKey="label"
                  tickLine={false}
                  axisLine={{ stroke: gridColor }}
                  tick={{ fill: axisColor, fontSize: 12 }}
                />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  width={64}
                  tick={{ fill: axisColor, fontSize: 12 }}
                  tickFormatter={(value: number) => compactCurrencyFormatter.format(value)}
                />
                <Tooltip content={<StackTooltip series={series} />} cursor={{ fill: gridColor, opacity: 0.4 }} />
                {series.map((serie, index) => (
                  <Bar
                    key={serie.key}
                    dataKey={serie.key}
                    name={serie.name}
                    stackId="revenue"
                    fill={serie.color}
                    stroke={surfaceColor}
                    strokeWidth={2}
                    maxBarSize={32}
                    radius={index === series.length - 1 ? [4, 4, 0, 0] : 0}
                  />
                ))}
              </BarChart>
            </ResponsiveContainer>
          </div>
          <ul className="mt-3 flex flex-wrap justify-center gap-x-4 gap-y-1 text-xs text-zinc-600 dark:text-zinc-300">
            {series.map((serie) => (
              <li key={serie.key} className="flex items-center gap-1.5">
                <span className="inline-block h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: serie.color }} aria-hidden />
                {serie.name}
              </li>
            ))}
          </ul>
        </>
      )}
    </Card>
  )
}

function describeSeries(series: StackedSeries[]): string {
  const clients = series.filter((serie) => serie.key !== OTHERS_KEY).length
  const hasOthers = clients < series.length
  if (clients === 0) return 'Receitas vinculadas a clientes no período.'
  const top = clients === 1 ? 'O maior cliente do período' : `Os ${clients} maiores clientes do período`
  return hasOthers ? `${top}; os demais somados em "Outros".` : `${top}.`
}

function StackTooltip({
  active,
  payload,
  label,
  series,
}: TooltipProps<number, string> & { series: StackedSeries[] }) {
  if (!active || !payload?.length) return null
  const total = payload.reduce((sum, entry) => sum + Number(entry.value ?? 0), 0)
  // Mesma ordem da legenda (do maior cliente para "Outros"), sem os meses zerados.
  const rows = series
    .map((serie) => ({ serie, value: Number(payload.find((entry) => entry.dataKey === serie.key)?.value ?? 0) }))
    .filter((row) => row.value > 0)

  return (
    <div className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-xs font-medium text-zinc-500 dark:text-zinc-400">
        {label} · {formatCurrency(total)}
      </p>
      <ul className="mt-1 space-y-1">
        {rows.map(({ serie, value }) => (
          <li key={serie.key} className="flex items-center gap-2 text-sm">
            <span className="inline-block h-2 w-2 rounded-sm" style={{ backgroundColor: serie.color }} aria-hidden />
            <span className="font-semibold text-zinc-800 dark:text-zinc-100">{formatCurrency(value)}</span>
            <span className="text-zinc-500 dark:text-zinc-400">{serie.name}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
