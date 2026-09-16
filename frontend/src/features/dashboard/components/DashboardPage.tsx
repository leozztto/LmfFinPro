import { StatCard } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useTransactions } from '@/features/transactions/hooks/useTransactions'

export function DashboardPage() {
  const { data: accounts, isLoading: loadingAccounts } = useAccounts()
  const { data: transactions, isLoading: loadingTransactions } = useTransactions()

  const isLoading = loadingAccounts || loadingTransactions

  const initialBalanceTotal = accounts?.reduce((sum, account) => sum + account.initialBalance, 0) ?? 0

  const currentYearMonth = getCurrentYearMonth()
  const monthTransactions = (transactions ?? []).filter((transaction) =>
    transaction.transactionDate.startsWith(currentYearMonth),
  )

  const incomeThisMonth = sumByType(monthTransactions, 'INCOME')
  const expenseThisMonth = sumByType(monthTransactions, 'EXPENSE')
  const totalIncome = sumByType(transactions ?? [], 'INCOME')
  const totalExpense = sumByType(transactions ?? [], 'EXPENSE')
  const currentBalance = initialBalanceTotal + totalIncome - totalExpense

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Visão geral</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">Saldo consolidado e movimento do mês.</p>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">Carregando...</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard label="Saldo atual" value={formatCurrency(currentBalance)} />
          <StatCard label="Receita do mês" value={formatCurrency(incomeThisMonth)} />
          <StatCard label="Despesa do mês" value={formatCurrency(expenseThisMonth)} />
        </div>
      )}
    </div>
  )
}

function sumByType(transactions: { type: string; amount: number }[], type: 'INCOME' | 'EXPENSE'): number {
  return transactions.filter((transaction) => transaction.type === type).reduce((sum, t) => sum + t.amount, 0)
}
