import { StatCard } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useDashboardOverview } from '../hooks/useDashboardOverview'
import { useMonthlyFlow } from '../hooks/useMonthlyFlow'
import { useBalanceEvolution } from '../hooks/useBalanceEvolution'
import { useCashFlowProjection } from '../hooks/useCashFlowProjection'
import { useCategoryBreakdown } from '../hooks/useCategoryBreakdown'
import { useClientBreakdown } from '../hooks/useClientBreakdown'
import { MonthlyFlowChart } from './MonthlyFlowChart'
import { BalanceEvolutionChart } from './BalanceEvolutionChart'
import { CashFlowProjectionChart } from './CashFlowProjectionChart'
import { BreakdownChart } from './BreakdownChart'
import { AccountBalanceChart } from './AccountBalanceChart'
import { toBalancePoints, toCashFlowProjectionPoints, toCategoryBreakdownPoints, toClientBreakdownPoints, toMonthlyFlowPoints } from '../utils'

export function DashboardPage() {
  const overview = useDashboardOverview()
  const monthlyFlow = useMonthlyFlow()
  const balanceEvolution = useBalanceEvolution()
  const cashFlowProjection = useCashFlowProjection()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const expenseByCategory = useCategoryBreakdown('EXPENSE')
  const incomeByCategory = useCategoryBreakdown('INCOME')
  const incomeByClient = useClientBreakdown()

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Visão geral</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">Saldo consolidado, movimento do mês e o que ainda está pendente.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          label="Saldo atual"
          value={overview.data ? formatCurrency(overview.data.currentBalance) : '…'}
          delta={overview.data?.balanceDeltaPercent != null ? { percent: overview.data.balanceDeltaPercent } : null}
        />
        <StatCard
          label="Receita do mês"
          value={overview.data ? formatCurrency(overview.data.currentMonthIncome) : '…'}
          delta={overview.data?.incomeDeltaPercent != null ? { percent: overview.data.incomeDeltaPercent } : null}
        />
        <StatCard
          label="Despesa do mês"
          value={overview.data ? formatCurrency(overview.data.currentMonthExpense) : '…'}
          delta={
            overview.data?.expenseDeltaPercent != null ? { percent: overview.data.expenseDeltaPercent, invert: true } : null
          }
        />
      </div>

      {/* Duas colunas já no celular (valores curtos); o saldo previsto ocupa a linha inteira. */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatCard
          label="A receber"
          value={overview.data ? formatCurrency(overview.data.pendingIncome) : '…'}
          hint="Receitas pendentes"
        />
        <StatCard
          label="A pagar"
          value={overview.data ? formatCurrency(overview.data.pendingExpense) : '…'}
          hint="Despesas pendentes"
        />
        <StatCard
          label="Saldo previsto"
          value={overview.data ? formatCurrency(overview.data.projectedBalance) : '…'}
          hint="Saldo atual + a receber − a pagar"
          className="col-span-2 sm:col-span-1"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {monthlyFlow.data ? (
          <MonthlyFlowChart data={toMonthlyFlowPoints(monthlyFlow.data)} />
        ) : (
          <ChartPlaceholder />
        )}
        {balanceEvolution.data ? (
          <BalanceEvolutionChart data={toBalancePoints(balanceEvolution.data)} />
        ) : (
          <ChartPlaceholder />
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {balanceEvolution.data && cashFlowProjection.data ? (
          <CashFlowProjectionChart
            data={[
              ...toBalancePoints(balanceEvolution.data).map((point) => ({ ...point, isProjected: false })),
              ...toCashFlowProjectionPoints(cashFlowProjection.data),
            ]}
          />
        ) : (
          <ChartPlaceholder />
        )}
        <AccountBalanceChart accounts={accounts ?? []} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {expenseByCategory.data && categories ? (
          <BreakdownChart
            title="Despesas por categoria"
            emptyMessage="Nenhuma despesa registrada neste mês ainda."
            data={toCategoryBreakdownPoints(expenseByCategory.data, categories)}
          />
        ) : (
          <ChartPlaceholder />
        )}
        {incomeByCategory.data && categories ? (
          <BreakdownChart
            title="Receita por categoria"
            emptyMessage="Nenhuma receita registrada neste mês ainda."
            data={toCategoryBreakdownPoints(incomeByCategory.data, categories)}
          />
        ) : (
          <ChartPlaceholder />
        )}
      </div>

      {incomeByClient.data && clients ? (
        <BreakdownChart
          title="Receita por cliente"
          emptyMessage="Nenhuma receita associada a um cliente neste mês ainda."
          data={toClientBreakdownPoints(incomeByClient.data, clients)}
        />
      ) : (
        <ChartPlaceholder />
      )}
    </div>
  )
}

function ChartPlaceholder() {
  return (
    <div className="flex h-56 items-center justify-center rounded-xl border border-dashed border-zinc-200 text-sm text-zinc-400 dark:border-zinc-700 dark:text-zinc-500">
      Carregando...
    </div>
  )
}
