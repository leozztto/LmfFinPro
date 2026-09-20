import { Card, IconButton } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useTaxEstimates } from '../hooks/useTaxEstimates'
import { useDeleteTaxEstimate } from '../hooks/useDeleteTaxEstimate'
import { TAX_REGIME_LABELS } from '../types'

export function TaxEstimateList() {
  const { data: taxEstimates, isLoading } = useTaxEstimates()
  const deleteTaxEstimate = useDeleteTaxEstimate()
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(id: number, monthLabel: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover a estimativa de "${monthLabel}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteTaxEstimate.mutate(id, {
      onSuccess: () => showToast('Estimativa removida com sucesso.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a estimativa.'),
    })
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando estimativas...</p>
  }

  if (!taxEstimates?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhuma estimativa cadastrada ainda. Adicione a primeira acima.
      </p>
    )
  }

  const sorted = [...taxEstimates].sort((a, b) => b.referenceMonth.localeCompare(a.referenceMonth))

  return (
    <div className="space-y-3">
      {sorted.map((taxEstimate) => {
        const monthLabel = formatMonthLabel(taxEstimate.referenceMonth)
        return (
          <Card key={taxEstimate.id} className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="font-medium text-zinc-800 dark:text-zinc-100">
                {monthLabel} · {TAX_REGIME_LABELS[taxEstimate.regime]}
              </p>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                Receita {formatCurrency(taxEstimate.grossRevenue)} · alíquota{' '}
                {(taxEstimate.appliedRate * 100).toFixed(2)}%
              </p>
              <p className="mt-1 text-sm font-semibold text-zinc-900 dark:text-zinc-50">
                {formatCurrency(taxEstimate.estimatedValue)}
              </p>
            </div>
            <IconButton
              icon={TrashIcon}
              label="Remover"
              onClick={() => handleDelete(taxEstimate.id, monthLabel)}
              disabled={deleteTaxEstimate.isPending}
              className="shrink-0"
            />
          </Card>
        )
      })}
    </div>
  )
}
