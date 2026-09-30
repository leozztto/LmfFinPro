import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { useSavingsGoals } from '../hooks/useSavingsGoals'
import type { SavingsGoal } from '../types'
import { ContributionForm } from './ContributionForm'
import { ContributionHistory } from './ContributionHistory'
import { SavingsGoalCard } from './SavingsGoalCard'
import { SavingsGoalForm } from './SavingsGoalForm'

type ModalState =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'edit'; goal: SavingsGoal }
  | { kind: 'contribute'; goal: SavingsGoal }
  | { kind: 'history'; goal: SavingsGoal }

const MODAL_TITLES: Record<Exclude<ModalState['kind'], 'closed'>, string> = {
  create: 'Nova meta',
  edit: 'Editar meta',
  contribute: 'Aporte ou resgate',
  history: 'Histórico da meta',
}

export function SavingsGoalsPage() {
  const { data: goals, isLoading } = useSavingsGoals()
  const [modal, setModal] = useState<ModalState>({ kind: 'closed' })
  const close = () => setModal({ kind: 'closed' })

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Metas de economia</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Caixinhas com valor-alvo e prazo: reserva de emergência, imposto, férias. Aporte e resgate são
            transferências reais entre a conta reserva e a conta de origem que você escolher ao criar a meta.
          </p>
        </div>
        <Button onClick={() => setModal({ kind: 'create' })} aria-label="Nova meta" title="Nova meta" className="px-3">
          <PlusIcon />
        </Button>
      </div>

      <Modal
        open={modal.kind !== 'closed'}
        onClose={close}
        title={modal.kind === 'closed' ? '' : modal.kind === 'history' ? `Histórico · ${modal.goal.name}` : MODAL_TITLES[modal.kind]}
      >
        {modal.kind === 'create' && <SavingsGoalForm onSuccess={close} />}
        {modal.kind === 'edit' && <SavingsGoalForm goal={modal.goal} onSuccess={close} />}
        {modal.kind === 'contribute' && <ContributionForm goal={modal.goal} onSuccess={close} />}
        {modal.kind === 'history' && <ContributionHistory goalId={modal.goal.id} />}
      </Modal>

      {isLoading ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando metas...</p>
      ) : !goals?.length ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma meta cadastrada ainda. Crie a primeira no botão acima — que tal uma caixinha do imposto?
        </p>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {goals.map((goal) => (
            <SavingsGoalCard
              key={goal.id}
              goal={goal}
              onEdit={() => setModal({ kind: 'edit', goal })}
              onContribute={() => setModal({ kind: 'contribute', goal })}
              onShowHistory={() => setModal({ kind: 'history', goal })}
            />
          ))}
        </div>
      )}
    </div>
  )
}
