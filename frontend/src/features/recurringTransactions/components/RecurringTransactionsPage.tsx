import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button, Modal, Tabs } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { RecurringBudgetBatchForm } from '@/features/budgets/components/RecurringBudgetBatchForm'
import { RecurringBudgetForm } from '@/features/budgets/components/RecurringBudgetForm'
import { RecurringBudgetList } from '@/features/budgets/components/RecurringBudgetList'
import { RecurringTransactionForm } from './RecurringTransactionForm'
import { RecurringTransactionList } from './RecurringTransactionList'

const TAB_PARAM = 'aba'
const BUDGETS_TAB = 'orcamentos'

const TAB_COPY = {
  transacoes: {
    modalTitle: 'Nova recorrência',
    description: 'Receitas e despesas que se repetem, lançadas automaticamente como transações em cada data.',
  },
  [BUDGETS_TAB]: {
    modalTitle: 'Novo orçamento recorrente',
    description: 'Categorias que ganham um orçamento novo automaticamente todo início de mês.',
  },
} as const

/** Recorrências de transações e de orçamentos, em abas (`/recorrencias?aba=orcamentos`). */
export function RecurringTransactionsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = searchParams.get(TAB_PARAM) === BUDGETS_TAB ? BUDGETS_TAB : 'transacoes'

  function handleTabChange(tabId: string) {
    setSearchParams(tabId === BUDGETS_TAB ? { [TAB_PARAM]: BUDGETS_TAB } : {}, { replace: true })
  }

  const copy = TAB_COPY[activeTab]

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Recorrências</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">{copy.description}</p>
        </div>
        <div className="flex shrink-0 gap-2">
          {activeTab === BUDGETS_TAB && (
            <Button variant="secondary" onClick={() => setIsBatchModalOpen(true)}>
              Criar em lote
            </Button>
          )}
          <Button
            onClick={() => setIsModalOpen(true)}
            aria-label={copy.modalTitle}
            title={copy.modalTitle}
            className="px-3"
          >
            <PlusIcon />
          </Button>
        </div>
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title={copy.modalTitle}>
        {activeTab === BUDGETS_TAB ? (
          <RecurringBudgetForm onSuccess={() => setIsModalOpen(false)} />
        ) : (
          <RecurringTransactionForm onSuccess={() => setIsModalOpen(false)} />
        )}
      </Modal>

      <Modal
        open={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        title="Criar orçamentos recorrentes em lote"
        size="lg"
      >
        <RecurringBudgetBatchForm onSuccess={() => setIsBatchModalOpen(false)} />
      </Modal>

      <Tabs
        activeTabId={activeTab}
        onTabChange={handleTabChange}
        tabs={[
          { id: 'transacoes', label: 'Transações', content: <RecurringTransactionList /> },
          { id: BUDGETS_TAB, label: 'Orçamentos', content: <RecurringBudgetList /> },
        ]}
      />
    </div>
  )
}
