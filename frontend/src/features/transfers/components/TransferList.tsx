import { Button, Card } from '@/shared/ui'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useTransfers } from '../hooks/useTransfers'
import { useDeleteTransfer } from '../hooks/useDeleteTransfer'

export function TransferList() {
  const { data: transfers, isLoading } = useTransfers()
  const { data: accounts } = useAccounts()
  const deleteTransfer = useDeleteTransfer()

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando transferências...</p>
  }

  if (!transfers?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma transferência registrada ainda.</p>
  }

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))
  const sorted = [...transfers].sort((a, b) => b.transferDate.localeCompare(a.transferDate))

  return (
    <div className="space-y-3">
      {sorted.map((transfer) => (
        <Card key={transfer.id} className="flex items-center justify-between gap-3">
          <div>
            <p className="font-medium text-zinc-900 dark:text-zinc-50">
              {accountNameById.get(transfer.fromAccountId) ?? 'conta desconhecida'} →{' '}
              {accountNameById.get(transfer.toAccountId) ?? 'conta desconhecida'}
            </p>
            <p className="text-sm text-zinc-500 dark:text-zinc-400">
              {formatDateOnlyBr(transfer.transferDate)}
              {transfer.description ? ` · ${transfer.description}` : ''}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="font-semibold text-zinc-900 dark:text-zinc-50">{formatCurrency(transfer.amount)}</span>
            <Button
              variant="secondary"
              onClick={() => deleteTransfer.mutate(transfer.id)}
              disabled={deleteTransfer.isPending}
            >
              Remover
            </Button>
          </div>
        </Card>
      ))}
    </div>
  )
}
