import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { AccountForm } from './AccountForm'
import { AccountList } from './AccountList'

export function AccountsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Contas</h2>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Contas bancárias e carteiras usadas para registrar suas transações.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>Nova conta</Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova conta">
        <AccountForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <AccountList />
    </div>
  )
}
