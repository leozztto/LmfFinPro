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

  // Transferências entre contas do próprio usuário não são receita nem despesa "real" —
  // excluídas de todos os somatórios para não inflar artificialmente o dashboard.
  const nonTransferTransactions = (transactions ?? []).filter((transaction) => transaction.transferId == null)

  const currentYearMonth = getCurrentYearMonth()
  const monthTransactions = nonTransferTransactions.filter((transaction) =>
    transaction.transactionDate.startsWith(currentYearMonth),
  )

  const incomeThisMonth = sumByType(monthTransactions, 'INCOME')
  const expenseThisMonth = sumByType(monthTransactions, 'EXPENSE')
  const totalIncome = sumByType(nonTransferTransactions, 'INCOME')
  const totalExpense = sumByType(nonTransferTransactions, 'EXPENSE')
  const currentBalance = initialBalanceTotal + totalIncome - totalExpense

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">Visão geral</h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Saldo consolidado e movimento do mês.</p>
      </div>

      {isLoading ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>
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
