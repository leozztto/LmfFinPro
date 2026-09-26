import { IconButton } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useDeleteContribution, useGoalContributions } from '../hooks/useSavingsGoals'

interface ContributionHistoryProps {
  goalId: number
}

export function ContributionHistory({ goalId }: ContributionHistoryProps) {
  const { data: contributions, isLoading } = useGoalContributions(goalId)
  const deleteContribution = useDeleteContribution()
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(contributionId: number) {
    const confirmed = await confirm({ message: 'Tem certeza que deseja excluir esta movimentação da meta?' })
    if (!confirmed) return

    deleteContribution.mutate(
      { goalId, contributionId },
      {
        onSuccess: () => showToast('Movimentação excluída.', 'success'),
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir a movimentação.'),
      },
    )
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando histórico...</p>
  }

  if (!contributions?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhum aporte registrado nesta meta ainda.</p>
  }

  return (
    <ul className="max-h-[60vh] divide-y divide-zinc-200 overflow-y-auto dark:divide-zinc-700">
      {contributions.map((contribution) => {
        const isDeposit = contribution.type === 'DEPOSIT'
        return (
          <li key={contribution.id} className="flex items-center justify-between gap-3 py-2">
            <div className="min-w-0">
              <p
                className={`text-sm font-medium ${
                  isDeposit ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                }`}
              >
                {isDeposit ? '+' : '−'} {formatCurrency(contribution.amount)}
                <span className="ml-2 font-normal text-zinc-500 dark:text-zinc-400">
                  {formatDateOnlyBr(contribution.contributionDate)}
                </span>
              </p>
              {contribution.note && (
                <p className="truncate text-xs text-zinc-500 dark:text-zinc-400" title={contribution.note}>
                  {contribution.note}
                </p>
              )}
            </div>
            <IconButton
              icon={TrashIcon}
              label="Excluir movimentação"
              onClick={() => handleDelete(contribution.id)}
              disabled={deleteContribution.isPending}
              className="shrink-0"
            />
          </li>
        )
      })}
    </ul>
  )
}
