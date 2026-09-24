import { Card, IconButton } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useBudgets } from '../hooks/useBudgets'
import { useDeleteBudget } from '../hooks/useDeleteBudget'
import { budgetUsage } from '../utils'

export function BudgetList() {
  const { data: budgets, isLoading } = useBudgets()
  const { data: categories } = useCategories()
  const deleteBudget = useDeleteBudget()
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(id: number, label: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover o orçamento de "${label}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteBudget.mutate(id, {
      onSuccess: () => showToast('Orçamento removido com sucesso.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível remover o orçamento.'),
    })
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando orçamentos...</p>
  }

  if (!budgets?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhum orçamento cadastrado ainda. Adicione o primeiro acima.</p>
  }

  const categoryById = new Map((categories ?? []).map((category) => [category.id, category]))
  const sorted = [...budgets].sort((a, b) => b.referenceMonth.localeCompare(a.referenceMonth))

  return (
    <div className="grid gap-3 lg:grid-cols-2">
      {sorted.map((budget) => {
        const category = categoryById.get(budget.categoryId)
        const categoryName = category?.name ?? 'Categoria removida'
        const monthLabel = formatMonthLabel(budget.referenceMonth)
        const spent = budget.spentValue
        const { percentage, isOverBudget, color } = budgetUsage(spent, budget.limitValue)

        return (
          <Card key={budget.id} className="flex items-center justify-between gap-3">
            <div className="min-w-0 flex-1">
              <p className="font-medium text-zinc-800 dark:text-zinc-100">
                {categoryName} · {monthLabel}
              </p>
              <div
                role="progressbar"
                aria-label={`Gasto de ${categoryName}`}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(percentage)}
                className="mt-2 h-2 w-full overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-700"
              >
                {/* Cor vai do azul claro ao verde (até 25%) e daí ao laranja; vermelho ao ultrapassar. */}
                <div
                  className="h-full rounded-full transition-all"
                  style={{ width: `${percentage}%`, backgroundColor: color }}
                />
              </div>
              <p
                className={`mt-1 text-xs ${
                  isOverBudget ? 'font-semibold text-red-600 dark:text-red-400' : 'text-zinc-500 dark:text-zinc-400'
                }`}
              >
                {formatCurrency(spent)} de {formatCurrency(budget.limitValue)}
                {isOverBudget && ' · limite ultrapassado'}
              </p>
            </div>
            <IconButton
              icon={TrashIcon}
              label="Remover"
              onClick={() => handleDelete(budget.id, `${categoryName} · ${monthLabel}`)}
              disabled={deleteBudget.isPending}
              className="shrink-0"
            />
          </Card>
        )
      })}
    </div>
  )
}
