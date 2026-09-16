import { AccountForm } from './AccountForm'
import { AccountList } from './AccountList'

export function AccountsPage() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Contas</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Contas bancárias e carteiras usadas para registrar suas transações.
        </p>
      </div>
      <AccountForm />
      <AccountList />
    </div>
  )
}
