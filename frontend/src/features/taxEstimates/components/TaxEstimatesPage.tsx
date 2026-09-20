import { useState } from 'react'
import { Button, Modal } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { TaxEstimateForm } from './TaxEstimateForm'
import { TaxEstimateList } from './TaxEstimateList'

export function TaxEstimatesPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Impostos</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Estimativa simplificada de imposto por mês e regime tributário.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} aria-label="Nova estimativa" title="Nova estimativa" className="px-3">
          <PlusIcon />
        </Button>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova estimativa de imposto">
        <TaxEstimateForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <TaxEstimateList />
    </div>
  )
}
