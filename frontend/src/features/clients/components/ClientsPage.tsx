import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { ClientForm } from './ClientForm'
import { ClientList } from './ClientList'

export function ClientsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Clientes</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Clientes e projetos aos quais você pode vincular receitas.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} aria-label="Novo cliente" title="Novo cliente" className="px-3">
          <PlusIcon />
        </Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Novo cliente">
        <ClientForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <ClientList />
    </div>
  )
}
