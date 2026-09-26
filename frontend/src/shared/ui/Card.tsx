import type { HTMLAttributes } from 'react'
import { ArrowDownIcon, ArrowUpIcon } from './icons'

const PADDING_CLASSES = { sm: 'p-3', md: 'p-5' } as const

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** "sm" para telas com muitos cards lado a lado (ex: dashboard). Padrão "md". */
  padding?: keyof typeof PADDING_CLASSES
}

export function Card({ className = '', padding = 'md', ...props }: CardProps) {
  return (
    <div
      className={`rounded-xl border border-zinc-200 bg-zinc-50 ${PADDING_CLASSES[padding]} shadow-sm dark:border-zinc-700 dark:bg-zinc-800 ${className}`}
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
  /** Linha de apoio abaixo do valor (ex: como ele é calculado). */
  hint?: string
  className?: string
}

export function StatCard({ label, value, delta, hint, className = '' }: StatCardProps) {
  return (
    <Card padding="sm" className={className}>
      <p className="text-sm text-zinc-500 dark:text-zinc-400">{label}</p>
      <p className="mt-0.5 text-xl font-semibold text-zinc-800 dark:text-zinc-100">{value}</p>
      {delta != null && <StatDeltaBadge {...delta} />}
      {hint && <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">{hint}</p>}
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
