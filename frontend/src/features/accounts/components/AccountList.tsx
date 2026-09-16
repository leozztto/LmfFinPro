import { Button, Card } from '@/shared/ui'
import { useAccounts } from '../hooks/useAccounts'
import { useDeleteAccount } from '../hooks/useDeleteAccount'
import { ACCOUNT_TYPE_LABELS } from '../types'
import { formatCurrency } from '@/shared/format/currency'

export function AccountList() {
  const { data: accounts, isLoading } = useAccounts()
  const deleteAccount = useDeleteAccount()

  if (isLoading) {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando contas...</p>
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">
        Nenhuma conta cadastrada ainda. Adicione a primeira acima.
      </p>
    )
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {accounts.map((account) => (
        <Card key={account.id} className="flex items-start justify-between gap-3">
          <div>
            <p className="font-medium text-slate-900 dark:text-slate-50">{account.name}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">{ACCOUNT_TYPE_LABELS[account.type]}</p>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
              Saldo inicial: {formatCurrency(account.initialBalance)}
            </p>
          </div>
          <Button
            variant="secondary"
            onClick={() => deleteAccount.mutate(account.id)}
            disabled={deleteAccount.isPending}
          >
            Remover
          </Button>
        </Card>
      ))}
    </div>
  )
}
