import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon, RepeatIcon } from '@/shared/ui/icons'
import { BudgetForm } from './BudgetForm'
import { BudgetList } from './BudgetList'

const RECURRING_BUDGETS_PATH = '/recorrencias?aba=orcamentos'

export function BudgetsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Orçamentos</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Metas de gasto por categoria e mês, comparadas com as despesas já lançadas. Para repetir um orçamento todo
            mês, use os{' '}
            <Link to={RECURRING_BUDGETS_PATH} className="font-medium text-primary-600 hover:underline dark:text-[#2ad6a5]">
              orçamentos recorrentes
            </Link>
            .
          </p>
        </div>
        <div className="flex shrink-0 gap-2">
          <Link
            to={RECURRING_BUDGETS_PATH}
            aria-label="Orçamentos recorrentes"
            title="Orçamentos recorrentes"
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600 dark:border-zinc-600 dark:bg-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-600"
          >
            <RepeatIcon className="h-4 w-4" />
            <span className="hidden sm:inline">Recorrentes</span>
          </Link>
          <Button onClick={() => setIsModalOpen(true)} aria-label="Novo orçamento" title="Novo orçamento" className="px-3">
            <PlusIcon />
          </Button>
        </div>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Novo orçamento">
        <BudgetForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <BudgetList />
    </div>
  )
}
