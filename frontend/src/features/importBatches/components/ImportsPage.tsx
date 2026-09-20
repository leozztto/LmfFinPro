import { Card, Tabs, type TabItem } from '@/shared/ui'
import { CategoryRulesPanel } from '@/features/categoryRules/components/CategoryRulesPanel'
import { ImportUploadForm } from './ImportUploadForm'
import { ImportBatchList } from './ImportBatchList'

const TABS: TabItem[] = [
  {
    id: 'importar',
    label: 'Importar arquivo',
    content: (
      <div className="space-y-8">
        <Card>
          <h3 className="mb-4 text-sm font-semibold text-zinc-800 dark:text-zinc-100">Nova importação</h3>
          <ImportUploadForm />
        </Card>

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

export function ImportsPage() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Importação de extrato</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Suba um CSV do banco e revise a categorização automática.
        </p>
      </div>

      <Tabs tabs={TABS} />
    </div>
  )
}
