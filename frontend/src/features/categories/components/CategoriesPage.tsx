import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { CategoryForm } from './CategoryForm'
import { CategoryList } from './CategoryList'

export function CategoriesPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Categorias</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Categorias padrão do sistema e as suas próprias, usadas para classificar transações.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>Nova categoria</Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova categoria">
        <CategoryForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <CategoryList />
    </div>
  )
}
