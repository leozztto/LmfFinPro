import type { HTMLAttributes } from 'react'
import { ArrowDownIcon, ArrowUpIcon } from './icons'

export function Card({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`rounded-xl border border-zinc-200 bg-zinc-50 p-4 shadow-sm dark:border-zinc-700 dark:bg-zinc-800 ${className}`}
      {...props}
    />
  )
}

interface StatDelta {
  /** Variação percentual vs. o período de comparação (pode ser negativa). */
  percent: number
  /** true quando um valor maior é uma notícia ruim (ex: despesa) — inverte as cores. */
  invert?: boolean
}

interface StatCardProps {
  label: string
  value: string
  delta?: StatDelta | null
}

export function StatCard({ label, value, delta }: StatCardProps) {
  return (
    <Card>
      <p className="text-sm text-zinc-500 dark:text-zinc-400">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-zinc-900 dark:text-zinc-50">{value}</p>
      {delta != null && <StatDeltaBadge {...delta} />}
    </Card>
  )
}

function StatDeltaBadge({ percent, invert = false }: StatDelta) {
  const isUp = percent >= 0
  const isGood = invert ? !isUp : isUp
  const colorClassName = isGood ? 'text-green-600 dark:text-green-500' : 'text-red-600 dark:text-red-500'

  return (
    <p className={`mt-1 flex items-center gap-1 text-xs font-medium ${colorClassName}`}>
      {isUp ? <ArrowUpIcon /> : <ArrowDownIcon />}
      <span>{Math.abs(Math.round(percent))}% vs. mês anterior</span>
    </p>
  )
}
