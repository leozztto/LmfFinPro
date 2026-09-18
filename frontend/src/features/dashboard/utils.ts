import type { Category } from '@/features/categories/types'
import type { Transaction } from '@/features/transactions/types'

const MONTH_LABELS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

const UNCATEGORIZED_COLOR = '#94a3b8'

export interface MonthlyFlowPoint {
  month: string
  label: string
  income: number
  expense: number
}

export interface CategoryBreakdownPoint {
  categoryId: number | null
  name: string
  color: string
  value: number
}

export function formatMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-')
  return `${MONTH_LABELS[Number(month) - 1]}/${year.slice(2)}`
}

/** Últimos `monthsCount` meses (formato "YYYY-MM"), do mais antigo para o atual. */
export function lastYearMonths(monthsCount: number): string[] {
  const result: string[] = []
  const now = new Date()
  for (let i = monthsCount - 1; i >= 0; i--) {
    const reference = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const month = String(reference.getMonth() + 1).padStart(2, '0')
    result.push(`${reference.getFullYear()}-${month}`)
  }
  return result
}

/**
 * Receita e despesa somadas por mês. Transferências entre contas próprias são
 * excluídas — não são receita nem despesa real, só movimento entre carteiras.
 */
export function buildMonthlyFlow(transactions: Transaction[], monthsCount = 6): MonthlyFlowPoint[] {
  const months = lastYearMonths(monthsCount)
  const totals = new Map<string, { income: number; expense: number }>(
    months.map((month) => [month, { income: 0, expense: 0 }]),
  )

  for (const transaction of transactions) {
    if (transaction.transferId != null) continue
    const bucket = totals.get(transaction.transactionDate.slice(0, 7))
    if (!bucket) continue
    if (transaction.type === 'INCOME') {
      bucket.income += transaction.amount
    } else {
      bucket.expense += transaction.amount
    }
  }

  return months.map((month) => {
    const bucket = totals.get(month)!
    return { month, label: formatMonthLabel(month), income: bucket.income, expense: bucket.expense }
  })
}

/** Despesas do mês informado, somadas por categoria e ordenadas da maior para a menor. */
export function buildExpenseByCategory(
  transactions: Transaction[],
  categories: Category[],
  yearMonth: string,
): CategoryBreakdownPoint[] {
  const totalsByCategory = new Map<number | null, number>()

  for (const transaction of transactions) {
    if (transaction.transferId != null) continue
    if (transaction.type !== 'EXPENSE') continue
    if (!transaction.transactionDate.startsWith(yearMonth)) continue

    const key = transaction.categoryId
    totalsByCategory.set(key, (totalsByCategory.get(key) ?? 0) + transaction.amount)
  }

  const categoryById = new Map(categories.map((category) => [category.id, category]))

  return Array.from(totalsByCategory.entries())
    .map(([categoryId, value]) => {
      const category = categoryId != null ? categoryById.get(categoryId) : undefined
      return {
        categoryId,
        name: category?.name ?? 'Sem categoria',
        color: category?.color ?? UNCATEGORIZED_COLOR,
        value,
      }
    })
    .sort((a, b) => b.value - a.value)
}
