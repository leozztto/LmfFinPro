import { useState } from 'react'
import { Card, IconButton, Modal } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useRecurringBudgets } from '../hooks/useRecurringBudgets'
import { useDeleteRecurringBudget } from '../hooks/useDeleteRecurringBudget'
import type { RecurringBudget } from '../types'
import { RecurringBudgetEditForm } from './RecurringBudgetEditForm'

function statusBadge(recurrence: RecurringBudget) {
  if (!recurrence.active) {
    return { label: 'Pausado', className: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300' }
  }
  if (recurrence.nextGenerationMonth == null) {
    return { label: 'Encerrado', className: 'bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-300' }
  }
  return null
}

export function RecurringBudgetList() {
  const { data: recurrences, isLoading } = useRecurringBudgets()
  const { data: categories } = useCategories()
  const deleteRecurringBudget = useDeleteRecurringBudget()
  const [editingRecurrence, setEditingRecurrence] = useState<RecurringBudget | null>(null)
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(recurrence: RecurringBudget, label: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover o orçamento recorrente de "${label}"? Os orçamentos já lançados serão mantidos.`,
    })
    if (!confirmed) return

    deleteRecurringBudget.mutate(recurrence.id, {
      onSuccess: () => showToast('Orçamento recorrente removido com sucesso.', 'success'),
      onError: (error) =>
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover o orçamento recorrente.'),
    })
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando orçamentos recorrentes...</p>
  }

  if (!recurrences?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhum orçamento recorrente cadastrado ainda. Adicione o primeiro acima.
      </p>
    )
  }

  const categoryById = new Map((categories ?? []).map((category) => [category.id, category.name]))

  return (
    <>
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        {recurrences.map((recurrence) => {
          const badge = statusBadge(recurrence)
          const categoryName = categoryById.get(recurrence.categoryId) ?? 'Categoria removida'

          return (
            <Card key={recurrence.id} className="flex flex-col gap-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="min-w-0 break-words font-medium text-zinc-800 dark:text-zinc-100">
                      {categoryName}
                    </p>
                    {badge && (
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${badge.className}`}>{badge.label}</span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                    {formatCurrency(recurrence.limitValue)} por mês
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <IconButton icon={PencilIcon} label="Editar" onClick={() => setEditingRecurrence(recurrence)} />
                  <IconButton
                    icon={TrashIcon}
                    label="Remover"
                    onClick={() => handleDelete(recurrence, categoryName)}
                    disabled={deleteRecurringBudget.isPending}
                  />
                </div>
              </div>
              <div className="border-t border-zinc-200 pt-3 text-xs text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
                {recurrence.nextGenerationMonth && (
                  <p>
                    Próximo: <span className="font-medium text-zinc-700 dark:text-zinc-200">{formatMonthLabel(recurrence.nextGenerationMonth)}</span>
                  </p>
                )}
                <p>
                  Desde {formatMonthLabel(recurrence.startMonth)}
                  {recurrence.endMonth && ` até ${formatMonthLabel(recurrence.endMonth)}`} · {recurrence.generatedMonths}{' '}
                  {recurrence.generatedMonths === 1 ? 'orçamento lançado' : 'orçamentos lançados'}
                </p>
              </div>
            </Card>
          )
        })}
      </div>

      <Modal open={editingRecurrence != null} onClose={() => setEditingRecurrence(null)} title="Editar orçamento recorrente">
        {editingRecurrence && (
          <RecurringBudgetEditForm
            key={editingRecurrence.id}
            recurrence={editingRecurrence}
            onSuccess={() => setEditingRecurrence(null)}
          />
        )}
      </Modal>
    </>
  )
}
