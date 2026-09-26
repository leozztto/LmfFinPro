import { useState } from 'react'
import { Card, IconButton, Modal } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useRecurringTransactions } from '../hooks/useRecurringTransactions'
import { useDeleteRecurringTransaction } from '../hooks/useDeleteRecurringTransaction'
import { RECURRENCE_FREQUENCY_LABELS, type RecurringTransaction } from '../types'
import { RecurringTransactionEditForm } from './RecurringTransactionEditForm'

function statusBadge(recurrence: RecurringTransaction) {
  if (!recurrence.active) {
    return { label: 'Pausada', className: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300' }
  }
  if (recurrence.nextOccurrenceDate == null) {
    return { label: 'Encerrada', className: 'bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300' }
  }
  return null
}

export function RecurringTransactionList() {
  const { data: recurrences, isLoading } = useRecurringTransactions()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const deleteRecurringTransaction = useDeleteRecurringTransaction()
  const [editingRecurrence, setEditingRecurrence] = useState<RecurringTransaction | null>(null)
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(recurrence: RecurringTransaction) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover a recorrência "${recurrence.description}"? As transações já lançadas serão mantidas.`,
    })
    if (!confirmed) return

    deleteRecurringTransaction.mutate(recurrence.id, {
      onSuccess: () => showToast('Recorrência removida com sucesso.', 'success'),
      onError: (error) =>
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a recorrência.'),
    })
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando recorrências...</p>
  }

  if (!recurrences?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhum lançamento recorrente cadastrado ainda. Adicione o primeiro acima.
      </p>
    )
  }

  const accountById = new Map((accounts ?? []).map((account) => [account.id, account.name]))
  const categoryById = new Map((categories ?? []).map((category) => [category.id, category.name]))
  const clientById = new Map((clients ?? []).map((client) => [client.id, client.name]))

  return (
    <>
      <div className="grid gap-3 lg:grid-cols-2">
        {recurrences.map((recurrence) => {
          const badge = statusBadge(recurrence)
          const isIncome = recurrence.type === 'INCOME'
          const details = [
            accountById.get(recurrence.accountId),
            recurrence.categoryId != null ? categoryById.get(recurrence.categoryId) : undefined,
            recurrence.clientId != null ? clientById.get(recurrence.clientId) : undefined,
          ]
            .filter(Boolean)
            .join(' · ')

          return (
            <Card key={recurrence.id} className="flex flex-col gap-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="min-w-0 break-words font-medium text-zinc-800 dark:text-zinc-100">
                      {recurrence.description}
                    </p>
                    <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200">
                      {RECURRENCE_FREQUENCY_LABELS[recurrence.frequency]}
                    </span>
                    {badge && (
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>{badge.label}</span>
                    )}
                  </div>
                  {details && <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">{details}</p>}
                </div>
                <span className={`shrink-0 font-semibold ${isIncome ? 'text-[#5ab482]' : 'text-[#f06464]'}`}>
                  {isIncome ? '+' : '-'} {formatCurrency(recurrence.amount)}
                </span>
              </div>
              <div className="flex flex-wrap items-end justify-between gap-2 border-t border-zinc-200 pt-3 text-xs text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
                <div className="space-y-0.5">
                  {recurrence.nextOccurrenceDate && (
                    <p>
                      Próxima: <span className="font-medium text-zinc-700 dark:text-zinc-200">{formatDateOnlyBr(recurrence.nextOccurrenceDate)}</span>
                    </p>
                  )}
                  <p>
                    Desde {formatDateOnlyBr(recurrence.startDate)}
                    {recurrence.endDate && ` até ${formatDateOnlyBr(recurrence.endDate)}`} · {recurrence.generatedOccurrences}{' '}
                    {recurrence.generatedOccurrences === 1 ? 'lançamento' : 'lançamentos'}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <IconButton icon={PencilIcon} label="Editar" onClick={() => setEditingRecurrence(recurrence)} />
                  <IconButton
                    icon={TrashIcon}
                    label="Remover"
                    onClick={() => handleDelete(recurrence)}
                    disabled={deleteRecurringTransaction.isPending}
                  />
                </div>
              </div>
            </Card>
          )
        })}
      </div>

      <Modal open={editingRecurrence != null} onClose={() => setEditingRecurrence(null)} title="Editar recorrência">
        {editingRecurrence && (
          <RecurringTransactionEditForm
            key={editingRecurrence.id}
            recurrence={editingRecurrence}
            onSuccess={() => setEditingRecurrence(null)}
          />
        )}
      </Modal>
    </>
  )
}
