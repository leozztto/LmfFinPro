import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type LegendProps,
  type TooltipProps,
} from 'recharts'
import { Card } from '@/shared/ui'
import { ArrowDownIcon, ArrowUpIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import type { MonthlyFlowPoint } from '../utils'

// Cores de status (não categóricas): receita = "bom", despesa = "mau". O par
// verde/vermelho não passa no teste de daltonismo por matiz sozinho, por isso
// nunca aparece sem o ícone de seta + o rótulo ao lado (ver legenda e tooltip).
const INCOME_COLOR = '#0ca30c'
const EXPENSE_COLOR = '#d03b3b'

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

interface MonthlyFlowChartProps {
  data: MonthlyFlowPoint[]
}

export function MonthlyFlowChart({ data }: MonthlyFlowChartProps) {
  const { theme } = useTheme()
  const gridColor = theme === 'dark' ? '#3f3f46' : '#e4e4e7'
  const axisColor = theme === 'dark' ? '#a1a1aa' : '#71717a'

  return (
    <Card padding="sm">
      <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Receita x despesa por mês</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">Últimos {data.length} meses, sem transferências.</p>
      <div className="mt-3 h-56">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 8, right: 8, left: 8, bottom: 0 }} barGap={2}>
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
            <Tooltip content={<FlowTooltip />} cursor={{ fill: gridColor, opacity: 0.4 }} />
            <Legend content={<FlowLegend />} />
            <Bar dataKey="income" name="Receita" fill={INCOME_COLOR} radius={[4, 4, 0, 0]} maxBarSize={24} />
            <Bar dataKey="expense" name="Despesa" fill={EXPENSE_COLOR} radius={[4, 4, 0, 0]} maxBarSize={24} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Card>
  )
}

function FlowTooltip({ active, payload, label }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null

  return (
    <div className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-xs font-medium text-zinc-500 dark:text-zinc-400">{label}</p>
      <ul className="mt-1 space-y-1">
        {payload.map((entry) => (
          <li key={entry.dataKey} className="flex items-center gap-2 text-sm">
            <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: entry.color }} aria-hidden />
            <span className="font-semibold text-zinc-800 dark:text-zinc-100">{formatCurrency(Number(entry.value))}</span>
            <span className="text-zinc-500 dark:text-zinc-400">{entry.name}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function FlowLegend({ payload }: LegendProps) {
  if (!payload) return null

  return (
    <ul className="mt-2 flex items-center justify-center gap-5 text-sm">
      {payload.map((entry) => (
        <li key={entry.dataKey as string} className="flex items-center gap-1.5 text-zinc-600 dark:text-zinc-300">
          <span className="inline-block h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: entry.color }} aria-hidden />
          {entry.dataKey === 'income' ? (
            <ArrowUpIcon className="h-3.5 w-3.5 text-current" />
          ) : (
            <ArrowDownIcon className="h-3.5 w-3.5 text-current" />
          )}
          <span>{entry.value}</span>
        </li>
      ))}
    </ul>
  )
}
