import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type DotProps,
  type TooltipProps,
} from 'recharts'
import { Card } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import type { BalancePoint } from '../utils'

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

interface BalanceEvolutionChartProps {
  data: BalancePoint[]
}

export function BalanceEvolutionChart({ data }: BalanceEvolutionChartProps) {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const gridColor = isDark ? '#3f3f46' : '#e4e4e7'
  const axisColor = isDark ? '#a1a1aa' : '#71717a'
  const surfaceColor = isDark ? '#27272a' : '#fafafa'
  const seriesColor = isDark ? '#3987e5' : '#2a78d6'

  return (
    <Card>
      <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">Evolução do saldo</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">Saldo consolidado ao fim de cada mês.</p>
      <div className="mt-4 h-72">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 24, right: 16, left: 8, bottom: 0 }}>
            <defs>
              <linearGradient id="balanceWash" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={seriesColor} stopOpacity={0.16} />
                <stop offset="100%" stopColor={seriesColor} stopOpacity={0} />
              </linearGradient>
            </defs>
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
            <Tooltip content={<BalanceTooltip />} cursor={{ stroke: axisColor, strokeWidth: 1 }} />
            <Area
              type="monotone"
              dataKey="balance"
              name="Saldo"
              stroke={seriesColor}
              strokeWidth={2}
              fill="url(#balanceWash)"
              dot={(props: DotProps & { index?: number }) => (
                <BalanceDot {...props} seriesColor={seriesColor} surfaceColor={surfaceColor} total={data.length} />
              )}
              activeDot={{ r: 5, fill: seriesColor, stroke: surfaceColor, strokeWidth: 2 }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  )
}

/** Marca todos os pontos com um dot discreto, mas só rotula o último — o saldo mais recente. */
function BalanceDot({
  cx,
  cy,
  index,
  total,
  seriesColor,
  surfaceColor,
  payload,
}: DotProps & { index?: number; total: number; seriesColor: string; surfaceColor: string; payload?: BalancePoint }) {
  if (cx == null || cy == null) return <g />
  const isLast = index === total - 1

  return (
    <g>
      <circle cx={cx} cy={cy} r={4} fill={seriesColor} stroke={surfaceColor} strokeWidth={2} />
      {isLast && payload && (
        <text
          x={cx}
          y={cy - 14}
          textAnchor="end"
          fontSize={12}
          fontWeight={600}
          className="fill-zinc-900 dark:fill-zinc-50"
        >
          {formatCurrency(payload.balance)}
        </text>
      )}
    </g>
  )
}

function BalanceTooltip({ active, payload, label }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const entry = payload[0]

  return (
    <div className="rounded-lg border border-zinc-200 bg-white px-3 py-2 shadow-md dark:border-zinc-700 dark:bg-zinc-900">
      <p className="text-xs font-medium text-zinc-500 dark:text-zinc-400">{label}</p>
      <div className="mt-1 flex items-center gap-2 text-sm">
        <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: entry.color }} aria-hidden />
        <span className="font-semibold text-zinc-900 dark:text-zinc-50">{formatCurrency(Number(entry.value))}</span>
        <span className="text-zinc-500 dark:text-zinc-400">Saldo</span>
      </div>
    </div>
  )
}
