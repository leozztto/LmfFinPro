import { TransactionForm } from './TransactionForm'
import { TransactionList } from './TransactionList'

export function TransactionsPage() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Transações</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">Extrato de receitas e despesas por conta.</p>
      </div>
      <TransactionForm />
      <TransactionList />
    </div>
  )
}
