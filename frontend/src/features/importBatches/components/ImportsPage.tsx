import { useState } from 'react'
import { Card, Tabs, type TabItem } from '@/shared/ui'
import { CategoryRulesPanel } from '@/features/categoryRules/components/CategoryRulesPanel'
import { ImportUploadForm } from './ImportUploadForm'
import { ImportBatchList } from './ImportBatchList'
import { ImportResultSummary } from './ImportResultSummary'
import type { ImportBatch } from '../types'

export function ImportsPage() {
  const [lastBatch, setLastBatch] = useState<ImportBatch | null>(null)

  const tabs: TabItem[] = [
    {
      id: 'importar',
      label: 'Importar arquivo',
      content: (
        <div className="space-y-8">
          <Card>
            <h3 className="mb-4 text-sm font-semibold text-zinc-800 dark:text-zinc-100">Nova importação</h3>
            <ImportUploadForm onSuccess={setLastBatch} />
          </Card>

          {lastBatch && <ImportResultSummary batchId={lastBatch.id} duplicateCount={lastBatch.duplicateCount} />}

          <ImportBatchList />
        </div>
      ),
    },
    {
      id: 'regras',
      label: 'Regras de categorização',
      content: <CategoryRulesPanel />,
    },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Importação de extrato</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Suba um CSV do banco e revise a categorização automática.
        </p>
      </div>

      <Tabs tabs={tabs} />
    </div>
  )
}
