import { useMemo } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { Card } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import type { CashFlowProjectionPoint } from '../utils'

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

interface CashFlowProjectionChartProps {
  /** Histórico real seguido dos meses projetados, concatenados em ordem cronológica. */
  data: CashFlowProjectionPoint[]
}

export function CashFlowProjectionChart({ data }: CashFlowProjectionChartProps) {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const gridColor = isDark ? '#3f3f46' : '#e4e4e7'
  const axisColor = isDark ? '#a1a1aa' : '#71717a'
  const actualColor = isDark ? '#3987e5' : '#2a78d6'
  const projectedColor = isDark ? '#c084fc' : '#9333ea'

  // O último ponto real recebe também o valor projetado, para as duas linhas se conectarem
  // visualmente no gráfico em vez de deixar uma quebra entre o histórico e a projeção.
  const chartData = useMemo(() => {
    const firstProjectedIndex = data.findIndex((point) => point.isProjected)
    const lastActualIndex = firstProjectedIndex === -1 ? -1 : firstProjectedIndex - 1
    return data.map((point, index) => ({
      label: point.label,
      isProjected: point.isProjected,
      actual: point.isProjected ? null : point.balance,
      projected: point.isProjected || index === lastActualIndex ? point.balance : null,
    }))
  }, [data])

  return (
    <Card>
      <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Projeção de fluxo de caixa</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Histórico recente e estimativa para os próximos meses (média móvel + lançamentos futuros já cadastrados).
      </p>
      <div className="mt-4 h-72">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 24, right: 16, left: 8, bottom: 0 }}>
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
              width={56}
              tick={{ fill: axisColor, fontSize: 12 }}
              tickFormatter={(value: number) => compactCurrencyFormatter.format(value)}
            />
            <Tooltip content={<ProjectionTooltip />} cursor={{ stroke: axisColor, strokeWidth: 1 }} />
            <Line
              type="monotone"
              dataKey="actual"
              name="Saldo"
              stroke={actualColor}
              strokeWidth={2}
              dot={{ r: 3, fill: actualColor }}
              activeDot={{ r: 5 }}
            />
            <Line
              type="monotone"
              dataKey="projected"
              name="Projetado"
              stroke={projectedColor}
              strokeWidth={2}
              strokeDasharray="5 5"
              dot={{ r: 3, fill: projectedColor }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <p className="mt-2 text-xs text-zinc-400 dark:text-zinc-500">
        Projeção estimada — não é garantia de saldo futuro.
      </p>
    </Card>
  )
}

function ProjectionTooltip({ active, payload, label }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const entry = payload.find((item) => item.value != null)
  if (!entry) return null
  const isProjected = (entry.payload as { isProjected: boolean }).isProjected

  return (
    <div className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-xs font-medium text-zinc-500 dark:text-zinc-400">{label}</p>
      <div className="mt-1 flex items-center gap-2 text-sm">
        <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: entry.color }} aria-hidden />
        <span className="font-semibold text-zinc-800 dark:text-zinc-100">{formatCurrency(Number(entry.value))}</span>
        <span className="text-zinc-500 dark:text-zinc-400">Saldo{isProjected ? ' (projetado)' : ''}</span>
      </div>
    </div>
  )
}
