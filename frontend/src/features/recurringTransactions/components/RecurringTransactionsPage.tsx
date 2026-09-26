import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { RecurringTransactionForm } from './RecurringTransactionForm'
import { RecurringTransactionList } from './RecurringTransactionList'

export function RecurringTransactionsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Recorrentes</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Receitas e despesas que se repetem, lançadas automaticamente como transações em cada data.
          </p>
        </div>
        <Button
          onClick={() => setIsModalOpen(true)}
          aria-label="Nova recorrência"
          title="Nova recorrência"
          className="px-3"
        >
          <PlusIcon />
        </Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova recorrência">
        <RecurringTransactionForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <RecurringTransactionList />
    </div>
  )
}
