import { useState } from 'react'
import { Card } from '@/shared/ui'
import { ChevronDownIcon } from '@/shared/ui/icons'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useImportBatches } from '../hooks/useImportBatches'
import { IMPORT_STATUS_LABELS, type ImportStatus } from '../types'
import { ImportBatchReviewTable } from './ImportBatchReviewTable'

const STATUS_BADGE_CLASSES: Record<ImportStatus, string> = {
  PENDING: 'bg-zinc-100 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300',
  PROCESSING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
  FAILED: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
}

export function ImportBatchList() {
  const { data: batches, isLoading } = useImportBatches()
  const { data: accounts } = useAccounts()
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando importações...</p>
  }

  if (!batches?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma importação realizada ainda.</p>
  }

  return (
    <div className="space-y-3">
      {batches.map((batch) => {
        const isExpanded = expandedId === batch.id
        return (
          <Card key={batch.id}>
            <button
              type="button"
              onClick={() => setExpandedId(isExpanded ? null : batch.id)}
              aria-expanded={isExpanded}
              className="flex w-full items-center justify-between gap-3 text-left"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="truncate font-medium text-zinc-800 dark:text-zinc-100">
                    {batch.originalFile ?? 'extrato.csv'}
                  </p>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE_CLASSES[batch.status]}`}>
                    {IMPORT_STATUS_LABELS[batch.status]}
                  </span>
                  {batch.uncategorizedCount > 0 && (
                    <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/40 dark:text-amber-300">
                      {batch.uncategorizedCount} sem categoria
                    </span>
                  )}
                </div>
                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                  {formatDateOnlyBr(batch.importedAt.slice(0, 10))} ·{' '}
                  {accountNameById.get(batch.accountId) ?? 'conta desconhecida'} · {batch.transactionCount} transações
                </p>
              </div>
              <ChevronDownIcon className={`h-4 w-4 shrink-0 transition-transform ${isExpanded ? 'rotate-180' : ''}`} />
            </button>

            {isExpanded && (
              <div className="mt-4 border-t border-zinc-200 pt-4 dark:border-zinc-800">
                <ImportBatchReviewTable batchId={batch.id} />
              </div>
            )}
          </Card>
        )
      })}
    </div>
  )
}
