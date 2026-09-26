import { Button, Card, IconButton } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useApplySuggestion, useDeleteSavingsGoal } from '../hooks/useSavingsGoals'
import type { SavingsGoal } from '../types'
import { GOAL_TYPE_LABELS, formatPercent, goalProgress } from '../utils'

interface SavingsGoalCardProps {
  goal: SavingsGoal
  onEdit: () => void
  onContribute: () => void
  onShowHistory: () => void
}

export function SavingsGoalCard({ goal, onEdit, onContribute, onShowHistory }: SavingsGoalCardProps) {
  const deleteGoal = useDeleteSavingsGoal()
  const applySuggestion = useApplySuggestion()
  const { showToast } = useToast()
  const confirm = useConfirm()

  const progress = goalProgress(goal.savedAmount, goal.targetAmount)
  const isReached = goal.remainingAmount <= 0

  async function handleDelete() {
    const confirmed = await confirm({
      message: `Tem certeza que deseja excluir a meta "${goal.name}" e todo o histórico de aportes? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteGoal.mutate(goal.id, {
      onSuccess: () => showToast('Meta excluída.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir a meta.'),
    })
  }

  function handleApplySuggestion() {
    applySuggestion.mutate(goal.id, {
      onSuccess: (contribution) =>
        showToast(`${formatCurrency(contribution.amount)} separados na meta.`, 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível separar o valor.'),
    })
  }

  return (
    <Card className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-medium text-zinc-800 dark:text-zinc-100" title={goal.name}>
            {goal.name}
          </p>
          <p className="text-xs text-zinc-500 dark:text-zinc-400">{GOAL_TYPE_LABELS[goal.type]}</p>
        </div>
        <div className="flex shrink-0 gap-1">
          <IconButton icon={PencilIcon} label="Editar meta" onClick={onEdit} />
          <IconButton icon={TrashIcon} label="Excluir meta" onClick={handleDelete} disabled={deleteGoal.isPending} />
        </div>
      </div>

      <div>
        <div
          role="progressbar"
          aria-label={`Progresso de ${goal.name}`}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(progress)}
          className="h-2 w-full overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-700"
        >
          <div
            className={`h-full rounded-full transition-all ${isReached ? 'bg-emerald-500' : 'bg-[#2ad6a5]'}`}
            style={{ width: `${progress}%` }}
          />
        </div>
        <p className="mt-1 flex flex-wrap justify-between gap-x-3 text-xs text-zinc-500 dark:text-zinc-400">
          <span>
            <span className="font-semibold text-zinc-800 dark:text-zinc-100">{formatCurrency(goal.savedAmount)}</span> de{' '}
            {formatCurrency(goal.targetAmount)}
          </span>
          <span>{Math.floor(progress)}%</span>
        </p>
      </div>

      <p className="text-sm text-zinc-600 dark:text-zinc-300">
        {isReached ? (
          <span className="font-medium text-emerald-600 dark:text-emerald-400">Meta atingida!</span>
        ) : (
          <>
            Faltam {formatCurrency(goal.remainingAmount)}
            {goal.deadline && ` até ${formatDateOnlyBr(goal.deadline)}`}
            {goal.monthlyNeeded !== null && ` · guarde ${formatCurrency(goal.monthlyNeeded)}/mês`}
          </>
        )}
      </p>

      {goal.incomeRate !== null && goal.suggestedContribution !== null && !isReached && (
        <div className="rounded-lg bg-[#2ad6a5]/10 p-3 text-sm text-zinc-700 dark:text-zinc-200">
          {goal.suggestedContribution > 0 ? (
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <p>
                Você recebeu {formatCurrency(goal.monthPaidIncome)} este mês. Separe {formatPercent(goal.incomeRate)}:{' '}
                <span className="font-semibold">{formatCurrency(goal.suggestedContribution)}</span>.
              </p>
              <Button
                variant="brand"
                className="w-full shrink-0 sm:w-auto"
                onClick={handleApplySuggestion}
                disabled={applySuggestion.isPending}
              >
                {applySuggestion.isPending ? 'Separando...' : `Separar ${formatCurrency(goal.suggestedContribution)}`}
              </Button>
            </div>
          ) : (
            <p>
              {goal.monthPaidIncome > 0
                ? `Os ${formatPercent(goal.incomeRate)} das receitas recebidas este mês já foram separados.`
                : `Nenhuma receita recebida este mês ainda. Quando entrar, a meta sugere separar ${formatPercent(goal.incomeRate)}.`}
            </p>
          )}
        </div>
      )}

      <div className="mt-auto flex flex-col gap-2 sm:flex-row">
        <Button variant="secondary" className="w-full sm:flex-1" onClick={onContribute}>
          Aporte / resgate
        </Button>
        <Button variant="secondary" className="w-full sm:flex-1" onClick={onShowHistory}>
          Histórico
        </Button>
      </div>
    </Card>
  )
}
