import { useState } from 'react'
import {
  CartesianGrid,
  Line,
  LineChart,
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
import { CATEGORICAL_PALETTE_DARK, CATEGORICAL_PALETTE_LIGHT } from '@/shared/chart/palette'
import type { NetWorthPoint } from '../types'
import { toChartPoints, type NetWorthChartPoint } from '../utils'

const compactCurrencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})

type SeriesKey = 'netWorth' | 'cash' | 'investments' | 'debts'

/** Ordem fixa da paleta categórica: a cor segue a série, nunca a posição. */
const SERIES: { key: SeriesKey; label: string; paletteIndex: number }[] = [
  { key: 'netWorth', label: 'Patrimônio líquido', paletteIndex: 0 },
  { key: 'cash', label: 'Contas', paletteIndex: 1 },
  { key: 'investments', label: 'Investimentos', paletteIndex: 2 },
  { key: 'debts', label: 'Dívidas', paletteIndex: 3 },
]

interface NetWorthChartProps {
  history: NetWorthPoint[]
}

export function NetWorthChart({ history }: NetWorthChartProps) {
  const { theme } = useTheme()
  const [showTable, setShowTable] = useState(false)
  const isDark = theme === 'dark'
  const palette = isDark ? CATEGORICAL_PALETTE_DARK : CATEGORICAL_PALETTE_LIGHT
  const gridColor = isDark ? '#3f3f46' : '#e4e4e7'
  const axisColor = isDark ? '#a1a1aa' : '#71717a'
  const surfaceColor = isDark ? '#27272a' : '#fafafa'
  const data = toChartPoints(history)
  const colorOf = (key: SeriesKey) => palette[SERIES.find((series) => series.key === key)!.paletteIndex]

  return (
    <Card padding="sm">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Evolução do patrimônio</h3>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            Contas + investimentos − dívidas ao fim de cada mês.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowTable((value) => !value)}
          aria-pressed={showTable}
          className="text-xs font-medium text-[#1ea883] hover:underline dark:text-[#2ad6a5]"
        >
          {showTable ? 'Ver gráfico' : 'Ver tabela'}
        </button>
      </div>

      <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-zinc-600 dark:text-zinc-300">
        {SERIES.map((series) => (
          <li key={series.key} className="inline-flex items-center gap-1.5">
            <span
              className="inline-block h-0.5 w-3 rounded-full"
              style={{ backgroundColor: colorOf(series.key) }}
              aria-hidden
            />
            {series.label}
          </li>
        ))}
      </ul>

      {showTable ? (
        <NetWorthTable data={data} />
      ) : (
        <div className="mt-2 h-64">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 24, right: 16, left: 8, bottom: 0 }}>
              <CartesianGrid vertical={false} stroke={gridColor} />
              <XAxis
                dataKey="label"
                tickLine={false}
                axisLine={{ stroke: gridColor }}
                tick={{ fill: axisColor, fontSize: 12 }}
                minTickGap={12}
              />
              <YAxis
                tickLine={false}
                axisLine={false}
                width={64}
                tick={{ fill: axisColor, fontSize: 12 }}
                tickFormatter={(value: number) => compactCurrencyFormatter.format(value)}
              />
              <Tooltip content={<NetWorthTooltip colorOf={colorOf} />} cursor={{ stroke: axisColor, strokeWidth: 1 }} />
              {SERIES.filter((series) => series.key !== 'netWorth').map((series) => (
                <Line
                  key={series.key}
                  type="monotone"
                  dataKey={series.key}
                  name={series.label}
                  stroke={colorOf(series.key)}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4, fill: colorOf(series.key), stroke: surfaceColor, strokeWidth: 2 }}
                />
              ))}
              {/* Por último, para ficar por cima: é a série principal. */}
              <Line
                type="monotone"
                dataKey="netWorth"
                name="Patrimônio líquido"
                stroke={colorOf('netWorth')}
                strokeWidth={2}
                dot={(props: DotProps & { index?: number }) => (
                  <LastValueDot
                    {...props}
                    total={data.length}
                    color={colorOf('netWorth')}
                    surfaceColor={surfaceColor}
                  />
                )}
                activeDot={{ r: 5, fill: colorOf('netWorth'), stroke: surfaceColor, strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  )
}

/** Só o último ponto do patrimônio ganha marcador e rótulo com o valor atual. */
function LastValueDot({
  cx,
  cy,
  index,
  total,
  color,
  surfaceColor,
  payload,
}: DotProps & { index?: number; total: number; color: string; surfaceColor: string; payload?: NetWorthChartPoint }) {
  if (cx == null || cy == null || index !== total - 1 || !payload) return <g />
  return (
    <g>
      <circle cx={cx} cy={cy} r={4} fill={color} stroke={surfaceColor} strokeWidth={2} />
      <text x={cx} y={cy - 14} textAnchor="end" fontSize={12} fontWeight={600} className="fill-zinc-900 dark:fill-zinc-50">
        {formatCurrency(payload.netWorth)}
      </text>
    </g>
  )
}

function NetWorthTooltip({
  active,
  payload,
  label,
  colorOf,
}: TooltipProps<number, string> & { colorOf: (key: SeriesKey) => string }) {
  if (!active || !payload?.length) return null
  const point = payload[0].payload as NetWorthChartPoint

  return (
    <div className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-xs font-medium text-zinc-500 dark:text-zinc-400">{label}</p>
      <ul className="mt-1 space-y-0.5 text-sm">
        {SERIES.map((series) => (
          <li key={series.key} className="flex items-center gap-2">
            <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: colorOf(series.key) }} aria-hidden />
            <span className="text-zinc-500 dark:text-zinc-400">{series.label}</span>
            <span
              className={`ml-auto pl-3 text-zinc-800 dark:text-zinc-100 ${series.key === 'netWorth' ? 'font-semibold' : ''}`}
            >
              {formatCurrency(point[series.key])}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function NetWorthTable({ data }: { data: NetWorthChartPoint[] }) {
  return (
    <div className="mt-3 max-h-72 overflow-auto">
      <table className="w-full min-w-[28rem] text-right text-sm">
        <thead className="sticky top-0 bg-zinc-50 text-xs text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400">
          <tr>
            <th className="py-1.5 text-left font-medium">Mês</th>
            {SERIES.map((series) => (
              <th key={series.key} className="py-1.5 pl-3 font-medium">
                {series.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-200 dark:divide-zinc-700">
          {[...data].reverse().map((point) => (
            <tr key={point.month} className="text-zinc-700 dark:text-zinc-200">
              <td className="py-1.5 text-left">{point.label}</td>
              <td className="py-1.5 pl-3 font-semibold text-zinc-800 dark:text-zinc-100">
                {formatCurrency(point.netWorth)}
              </td>
              <td className="py-1.5 pl-3">{formatCurrency(point.cash)}</td>
              <td className="py-1.5 pl-3">{formatCurrency(point.investments)}</td>
              <td className="py-1.5 pl-3">{formatCurrency(point.debts)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
