import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { Card } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useTheme } from '@/shared/theme/ThemeContext'
import { ACCOUNT_TYPE_LABELS, type Account } from '@/features/accounts/types'

// Paleta categórica fixa do design system (ordem estável — identidade da conta,
// nunca reordenada pelo valor do saldo). Ver docs/plano.md e a skill de dataviz.
const PALETTE_LIGHT = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#4a3aa7', '#e34948']
const PALETTE_DARK = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#008300', '#9085e9', '#e66767']

interface AccountBalancePoint {
  accountId: number
  name: string
  type: Account['type']
  balance: number
  color: string
}

interface AccountBalanceChartProps {
  accounts: Account[]
}

export function AccountBalanceChart({ accounts }: AccountBalanceChartProps) {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const axisColor = isDark ? '#a1a1aa' : '#71717a'
  const labelColor = isDark ? '#e4e4e7' : '#3f3f46'
  const palette = isDark ? PALETTE_DARK : PALETTE_LIGHT

  const bars: AccountBalancePoint[] = accounts.map((account, index) => ({
    accountId: account.id,
    name: account.name,
    type: account.type,
    balance: account.currentBalance,
    color: palette[index % palette.length],
  }))

  const height = Math.max(bars.length * 40 + 24, 120)
  const maxAbs = Math.max(1, ...bars.map((bar) => Math.abs(bar.balance)))
  const hasNegative = bars.some((bar) => bar.balance < 0)

  return (
    <Card>
      <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">Saldo por conta</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">Saldo atual de cada conta cadastrada.</p>

      {bars.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">Nenhuma conta cadastrada ainda.</p>
      ) : (
        <div className="mt-4" style={{ height }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={bars} layout="vertical" margin={{ top: 4, right: 72, left: 8, bottom: 4 }}>
              <XAxis type="number" hide domain={[hasNegative ? -maxAbs * 1.2 : 0, maxAbs * 1.2]} />
              <YAxis
                type="category"
                dataKey="name"
                width={112}
                tickLine={false}
                axisLine={false}
                tick={{ fill: axisColor, fontSize: 12 }}
              />
              <Tooltip content={<AccountTooltip />} cursor={{ fill: axisColor, opacity: 0.12 }} />
              <Bar dataKey="balance" radius={[0, 4, 4, 0]} maxBarSize={24}>
                {bars.map((entry) => (
                  <Cell key={entry.accountId} fill={entry.color} />
                ))}
                <LabelList dataKey="balance" content={(props) => <LabelAtTip {...props} fill={labelColor} />} />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  )
}

interface LabelAtTipProps {
  fill: string
  x?: number | string
  y?: number | string
  width?: number | string
  height?: number | string
  value?: number | string
}

/** Rótulo de valor na ponta da barra — à direita quando positiva, à esquerda quando negativa. */
function LabelAtTip({ fill, x = 0, y = 0, width = 0, height = 0, value = 0 }: LabelAtTipProps) {
  const numX = Number(x)
  const numY = Number(y)
  const numWidth = Number(width)
  const numHeight = Number(height)
  const amount = Number(value)
  const isNegative = amount < 0
  const labelX = isNegative ? numX - 8 : numX + numWidth + 8

  return (
    <text x={labelX} y={numY + numHeight / 2} dy={4} textAnchor={isNegative ? 'end' : 'start'} fontSize={12} fill={fill}>
      {formatCurrency(amount)}
    </text>
  )
}

function AccountTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const entry = payload[0]
  const point = entry.payload as AccountBalancePoint

  return (
    <div className="rounded-lg border border-zinc-200 bg-white px-3 py-2 shadow-md dark:border-zinc-700 dark:bg-zinc-900">
      <div className="flex items-center gap-2 text-sm">
        <span className="inline-block h-0.5 w-3 rounded-full" style={{ backgroundColor: point.color }} aria-hidden />
        <span className="font-semibold text-zinc-900 dark:text-zinc-50">{formatCurrency(Number(entry.value))}</span>
        <span className="text-zinc-500 dark:text-zinc-400">
          {point.name} · {ACCOUNT_TYPE_LABELS[point.type]}
        </span>
      </div>
    </div>
  )
}
