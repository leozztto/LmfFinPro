import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { TransferForm } from './TransferForm'
import { TransferList } from './TransferList'

export function TransfersPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Transferências</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">Mova valores entre suas próprias contas.</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>Nova transferência</Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova transferência">
        <TransferForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <TransferList />
    </div>
  )
}
