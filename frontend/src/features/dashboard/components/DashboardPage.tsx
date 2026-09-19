import { StatCard } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useTransactions } from '@/features/transactions/hooks/useTransactions'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { MonthlyFlowChart } from './MonthlyFlowChart'
import { BalanceEvolutionChart } from './BalanceEvolutionChart'
import { BreakdownChart } from './BreakdownChart'
import { AccountBalanceChart } from './AccountBalanceChart'
import { buildBalanceOverTime, buildCategoryBreakdown, buildClientBreakdown, buildMonthlyFlow, computeDeltaPercent } from '../utils'

export function DashboardPage() {
  const { data: accounts, isLoading: loadingAccounts } = useAccounts()
  const { data: transactions, isLoading: loadingTransactions } = useTransactions()
  const { data: categories, isLoading: loadingCategories } = useCategories()
  const { data: clients, isLoading: loadingClients } = useClients()

  const isLoading = loadingAccounts || loadingTransactions || loadingCategories || loadingClients

  const initialBalanceTotal = accounts?.reduce((sum, account) => sum + account.initialBalance, 0) ?? 0

  // Transferências entre contas do próprio usuário não são receita nem despesa "real" —
  // excluídas de todos os somatórios para não inflar artificialmente o dashboard.
  const nonTransferTransactions = (transactions ?? []).filter((transaction) => transaction.transferId == null)

  const currentYearMonth = getCurrentYearMonth()
  const totalIncome = sumByType(nonTransferTransactions, 'INCOME')
  const totalExpense = sumByType(nonTransferTransactions, 'EXPENSE')
  const currentBalance = initialBalanceTotal + totalIncome - totalExpense

  const monthlyFlow = buildMonthlyFlow(nonTransferTransactions)
  const currentMonth = monthlyFlow[monthlyFlow.length - 1]
  const previousMonth = monthlyFlow[monthlyFlow.length - 2]

  const balanceOverTime = buildBalanceOverTime(nonTransferTransactions, initialBalanceTotal)
  const previousBalance = balanceOverTime[balanceOverTime.length - 2]?.balance

  const expenseByCategory = buildCategoryBreakdown(nonTransferTransactions, categories ?? [], currentYearMonth, 'EXPENSE')
  const incomeByCategory = buildCategoryBreakdown(nonTransferTransactions, categories ?? [], currentYearMonth, 'INCOME')
  const incomeByClient = buildClientBreakdown(nonTransferTransactions, clients ?? [], currentYearMonth)

  const balanceDeltaPercent = computeDeltaPercent(currentBalance, previousBalance)
  const incomeDeltaPercent = computeDeltaPercent(currentMonth.income, previousMonth.income)
  const expenseDeltaPercent = computeDeltaPercent(currentMonth.expense, previousMonth.expense)

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Visão geral</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">Saldo consolidado e movimento do mês.</p>
      </div>

      {isLoading ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
            <StatCard
              label="Saldo atual"
              value={formatCurrency(currentBalance)}
              delta={balanceDeltaPercent != null ? { percent: balanceDeltaPercent } : null}
            />
            <StatCard
              label="Receita do mês"
              value={formatCurrency(currentMonth.income)}
              delta={incomeDeltaPercent != null ? { percent: incomeDeltaPercent } : null}
            />
            <StatCard
              label="Despesa do mês"
              value={formatCurrency(currentMonth.expense)}
              delta={expenseDeltaPercent != null ? { percent: expenseDeltaPercent, invert: true } : null}
            />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <MonthlyFlowChart data={monthlyFlow} />
            <BalanceEvolutionChart data={balanceOverTime} />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <BreakdownChart
              title="Despesas por categoria"
              emptyMessage="Nenhuma despesa registrada neste mês ainda."
              data={expenseByCategory}
            />
            <BreakdownChart
              title="Receita por categoria"
              emptyMessage="Nenhuma receita registrada neste mês ainda."
              data={incomeByCategory}
            />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <BreakdownChart
              title="Receita por cliente"
              emptyMessage="Nenhuma receita associada a um cliente neste mês ainda."
              data={incomeByClient}
            />
            <AccountBalanceChart accounts={accounts ?? []} />
          </div>
        </>
      )}
    </div>
  )
}

function sumByType(transactions: { type: string; amount: number }[], type: 'INCOME' | 'EXPENSE'): number {
  return transactions.filter((transaction) => transaction.type === type).reduce((sum, t) => sum + t.amount, 0)
}
