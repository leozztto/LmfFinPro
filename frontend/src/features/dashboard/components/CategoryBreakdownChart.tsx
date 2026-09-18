import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { Card } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import type { CategoryBreakdownPoint } from '../utils'

const OTHER_COLOR = '#71717a'
const MAX_BARS = 7

interface CategoryBreakdownChartProps {
  title: string
  emptyMessage: string
  data: CategoryBreakdownPoint[]
}

export function CategoryBreakdownChart({ title, emptyMessage, data }: CategoryBreakdownChartProps) {
  const { theme } = useTheme()
  const axisColor = theme === 'dark' ? '#a1a1aa' : '#71717a'

  const bars = foldIntoOther(data)
  const height = Math.max(bars.length * 40 + 24, 120)

  return (
    <Card>
      <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">{title}</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">Mês atual.</p>

      {bars.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">{emptyMessage}</p>
      ) : (
        <div className="mt-4" style={{ height }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={bars} layout="vertical" margin={{ top: 4, right: 72, left: 8, bottom: 4 }}>
              <XAxis type="number" hide domain={[0, (max: number) => max * 1.2]} />
              <YAxis
                type="category"
                dataKey="name"
                width={112}
                tickLine={false}
                axisLine={false}
                tick={{ fill: axisColor, fontSize: 12 }}
              />
              <Tooltip content={<BreakdownTooltip />} cursor={{ fill: axisColor, opacity: 0.12 }} />
              <Bar dataKey="value" radius={[0, 4, 4, 0]} maxBarSize={24}>
                {bars.map((entry) => (
                  <Cell key={entry.name} fill={entry.color} />
                ))}
                <LabelList
                  dataKey="value"
                  position="right"
                  formatter={(value: number) => formatCurrency(value)}
                  fill={theme === 'dark' ? '#e4e4e7' : '#3f3f46'}
                  fontSize={12}
                />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  )
}

function BreakdownTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const entry = payload[0]
  const point = entry.payload as CategoryBreakdownPoint

  return (
    <div className="rounded-lg border border-zinc-200 bg-white px-3 py-2 shadow-md dark:border-zinc-700 dark:bg-zinc-900">
      <div className="flex items-center gap-2 text-sm">
        <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: point.color }} aria-hidden />
        <span className="font-semibold text-zinc-900 dark:text-zinc-50">{formatCurrency(Number(entry.value))}</span>
        <span className="text-zinc-500 dark:text-zinc-400">{point.name}</span>
      </div>
    </div>
  )
}

/** Mantém as maiores categorias como barras individuais e soma o restante em "Outras". */
function foldIntoOther(data: CategoryBreakdownPoint[]): CategoryBreakdownPoint[] {
  if (data.length <= MAX_BARS) return data

  const head = data.slice(0, MAX_BARS - 1)
  const tail = data.slice(MAX_BARS - 1)
  const otherTotal = tail.reduce((sum, item) => sum + item.value, 0)

  return [...head, { categoryId: null, name: 'Outras', color: OTHER_COLOR, value: otherTotal }]
}
