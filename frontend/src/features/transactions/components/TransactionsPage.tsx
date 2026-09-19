import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { TransactionForm } from './TransactionForm'
import { TransactionList } from './TransactionList'

export function TransactionsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Transações</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">Extrato de receitas e despesas por conta.</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} aria-label="Nova transação" title="Nova transação" className="px-3">
          <PlusIcon />
        </Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Lançar transação">
        <TransactionForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <TransactionList />
    </div>
  )
}
