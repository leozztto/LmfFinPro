import type { Category, CategoryType } from '@/features/categories/types'
import type { Client } from '@/features/clients/types'
import type { Transaction } from '@/features/transactions/types'

const MONTH_LABELS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

const UNASSIGNED_COLOR = '#94a3b8'

export interface MonthlyFlowPoint {
  month: string
  label: string
  income: number
  expense: number
}

/** Ponto de um gráfico de barras "total por entidade" (categoria, cliente, etc.). */
export interface BreakdownPoint {
  id: number | null
  name: string
  color: string
  value: number
}

export interface BalancePoint {
  month: string
  label: string
  balance: number
}

export interface CashFlowProjectionPoint extends BalancePoint {
  /** true para meses futuros (projetados); false para o histórico real. */
  isProjected: boolean
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

/** Próximos `monthsCount` meses (formato "YYYY-MM") após o mês atual, do mais próximo ao mais distante. */
export function nextYearMonths(monthsCount: number): string[] {
  const result: string[] = []
  const now = new Date()
  for (let i = 1; i <= monthsCount; i++) {
    const reference = new Date(now.getFullYear(), now.getMonth() + i, 1)
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

/**
 * Saldo consolidado (todas as contas) ao final de cada um dos últimos `monthsCount` meses —
 * saldo inicial total mais o acumulado de receita/despesa até aquele mês, inclusive.
 */
export function buildBalanceOverTime(
  transactions: Transaction[],
  initialBalanceTotal: number,
  monthsCount = 6,
): BalancePoint[] {
  const months = lastYearMonths(monthsCount)
  const nonTransfer = transactions.filter((transaction) => transaction.transferId == null)

  return months.map((month) => {
    const balance = nonTransfer.reduce((sum, transaction) => {
      if (transaction.transactionDate.slice(0, 7) > month) return sum
      return sum + (transaction.type === 'INCOME' ? transaction.amount : -transaction.amount)
    }, initialBalanceTotal)
    return { month, label: formatMonthLabel(month), balance }
  })
}

/**
 * Projeção simplificada de fluxo de caixa para os próximos `monthsAhead` meses, a partir do
 * saldo atual: para um mês futuro que já tem transações cadastradas (recebíveis/despesas já
 * lançadas), usa o líquido real desse mês; senão, usa a média móvel do líquido dos últimos 3
 * meses como estimativa. Não é garantia — só uma tendência baseada no histórico recente.
 */
export function buildCashFlowProjection(
  transactions: Transaction[],
  currentBalance: number,
  monthsAhead = 3,
): CashFlowProjectionPoint[] {
  const nonTransfer = transactions.filter((transaction) => transaction.transferId == null)

  const recentFlow = buildMonthlyFlow(transactions, 3)
  const averageNet = recentFlow.reduce((sum, point) => sum + (point.income - point.expense), 0) / recentFlow.length

  let balance = currentBalance
  return nextYearMonths(monthsAhead).map((month) => {
    const monthTransactions = nonTransfer.filter((transaction) => transaction.transactionDate.startsWith(month))
    const net =
      monthTransactions.length > 0
        ? monthTransactions.reduce((sum, t) => sum + (t.type === 'INCOME' ? t.amount : -t.amount), 0)
        : averageNet
    balance += net
    return { month, label: formatMonthLabel(month), balance, isProjected: true }
  })
}

/** Transações do tipo/mês informados, somadas por categoria e ordenadas da maior para a menor. */
export function buildCategoryBreakdown(
  transactions: Transaction[],
  categories: Category[],
  yearMonth: string,
  type: CategoryType,
): BreakdownPoint[] {
  const totalsByCategory = new Map<number | null, number>()

  for (const transaction of transactions) {
    if (transaction.transferId != null) continue
    if (transaction.type !== type) continue
    if (!transaction.transactionDate.startsWith(yearMonth)) continue

    const key = transaction.categoryId
    totalsByCategory.set(key, (totalsByCategory.get(key) ?? 0) + transaction.amount)
  }

  const categoryById = new Map(categories.map((category) => [category.id, category]))

  return Array.from(totalsByCategory.entries())
    .map(([categoryId, value]) => {
      const category = categoryId != null ? categoryById.get(categoryId) : undefined
      return {
        id: categoryId,
        name: category?.name ?? 'Sem categoria',
        color: category?.color ?? UNASSIGNED_COLOR,
        value,
      }
    })
    .sort((a, b) => b.value - a.value)
}

/** Receita do mês informado, somada por cliente e ordenada da maior para a menor. */
export function buildClientBreakdown(transactions: Transaction[], clients: Client[], yearMonth: string): BreakdownPoint[] {
  const totalsByClient = new Map<number | null, number>()

  for (const transaction of transactions) {
    if (transaction.transferId != null) continue
    if (transaction.type !== 'INCOME') continue
    if (!transaction.transactionDate.startsWith(yearMonth)) continue

    const key = transaction.clientId
    totalsByClient.set(key, (totalsByClient.get(key) ?? 0) + transaction.amount)
  }

  const clientById = new Map(clients.map((client) => [client.id, client]))

  return Array.from(totalsByClient.entries())
    .map(([clientId, value]) => {
      const client = clientId != null ? clientById.get(clientId) : undefined
      return {
        id: clientId,
        name: client?.name ?? 'Sem cliente',
        color: client?.color ?? UNASSIGNED_COLOR,
        value,
      }
    })
    .sort((a, b) => b.value - a.value)
}

/**
 * Variação percentual entre dois períodos, para o indicador "vs mês anterior" dos cards.
 * Retorna `null` quando o período anterior é zero — a variação percentual não tem
 * leitura útil nesse caso (evita mostrar "+∞%" ou um número artificial).
 */
export function computeDeltaPercent(current: number, previous: number): number | null {
  if (previous === 0) return null
  return ((current - previous) / Math.abs(previous)) * 100
}
