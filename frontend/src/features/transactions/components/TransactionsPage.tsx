import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { TransactionForm } from './TransactionForm'
import { TransactionList } from './TransactionList'

export function TransactionsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Transações</h2>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Extrato de receitas e despesas por conta.</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>Nova transação</Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Lançar transação">
        <TransactionForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <TransactionList />
    </div>
  )
}
