import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { BudgetForm } from './BudgetForm'
import { BudgetList } from './BudgetList'

export function BudgetsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Orçamentos</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Metas de gasto por categoria e mês, comparadas com as despesas já lançadas.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} aria-label="Novo orçamento" title="Novo orçamento" className="px-3">
          <PlusIcon />
        </Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Novo orçamento">
        <BudgetForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <BudgetList />
    </div>
  )
}
