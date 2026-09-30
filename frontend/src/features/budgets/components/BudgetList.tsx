import { useState } from 'react'
import { Card, IconButton, Select } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useBudgets } from '../hooks/useBudgets'
import { useDeleteBudget } from '../hooks/useDeleteBudget'
import { budgetUsage } from '../utils'

export function BudgetList() {
  const { data: budgets, isLoading } = useBudgets()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  // '' = todos; 'NONE' = só orçamentos gerais; senão, o id do cliente
  const [clientFilter, setClientFilter] = useState('')
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
  const clientById = new Map((clients ?? []).map((client) => [client.id, client]))
  const hasClientBudgets = budgets.some((budget) => budget.clientId !== null)
  const visible = budgets.filter((budget) => {
    if (clientFilter === '') return true
    if (clientFilter === 'NONE') return budget.clientId === null
    return budget.clientId === Number(clientFilter)
  })
  const sorted = [...visible].sort((a, b) => b.referenceMonth.localeCompare(a.referenceMonth))

  return (
    <div className="space-y-4">
      {(hasClientBudgets || clientFilter !== '') && (
        <div className="sm:max-w-xs">
          <Select aria-label="Filtrar por cliente" value={clientFilter} onChange={(event) => setClientFilter(event.target.value)}>
            <option value="">Todos os orçamentos</option>
            <option value="NONE">Sem cliente (gerais)</option>
            {(clients ?? []).filter((client) => budgets.some((budget) => budget.clientId === client.id)).map((client) => (
              <option key={client.id} value={client.id}>
                {client.name}
              </option>
            ))}
          </Select>
        </div>
      )}
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
      {sorted.map((budget) => {
        const category = categoryById.get(budget.categoryId)
        const categoryName = category?.name ?? 'Categoria removida'
        const clientName = budget.clientId === null ? null : (clientById.get(budget.clientId)?.name ?? 'Cliente removido')
        const monthLabel = formatMonthLabel(budget.referenceMonth)
        const spent = budget.spentValue
        const { percentage, isOverBudget, color } = budgetUsage(spent, budget.limitValue)

        return (
          <Card key={budget.id} className="flex items-center justify-between gap-3">
            <div className="min-w-0 flex-1">
              <p className="font-medium text-zinc-800 dark:text-zinc-100">
                {categoryName} · {monthLabel}
              </p>
              <p className="truncate text-xs text-zinc-500 dark:text-zinc-400">
                {clientName ?? 'Orçamento geral'}
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
              onClick={() => handleDelete(budget.id, `${categoryName} · ${monthLabel}${clientName ? ` · ${clientName}` : ''}`)}
              disabled={deleteBudget.isPending}
              className="shrink-0"
            />
          </Card>
        )
      })}
      {sorted.length === 0 && (
        <p className="text-sm text-zinc-500 dark:text-zinc-400 lg:col-span-2">Nenhum orçamento para este filtro.</p>
      )}
      </div>
    </div>
  )
}
