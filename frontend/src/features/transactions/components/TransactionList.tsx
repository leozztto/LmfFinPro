import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button, CollapsibleFilters, FormField, Input, Modal, Pagination, Select } from '@/shared/ui'
import { TransactionAttachmentsPanel } from '@/features/attachments/components/TransactionAttachmentsPanel'
import { TagInput } from '@/features/tags/components/TagInput'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useTransactions } from '../hooks/useTransactions'
import { useDeleteTransaction } from '../hooks/useDeleteTransaction'
import { useUpdateTransactionStatus } from '../hooks/useUpdateTransactionStatus'
import {
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPE_LABELS,
  type Transaction,
  type TransactionStatus,
  type TransactionListParams,
  type TransactionType,
} from '../types'
import { TransactionCard } from './TransactionCard'
import { TransactionTagsEditor } from './TransactionTagsEditor'

interface Filters {
  accountId: string
  categoryId: string
  clientId: string
  type: TransactionType | ''
  status: TransactionStatus | ''
  attachment: 'WITH' | 'WITHOUT' | ''
  /** Nomes das tags: entra a transação que tiver qualquer uma delas. */
  tags: string[]
  startDate: string
  endDate: string
}

const EMPTY_FILTERS: Filters = {
  accountId: '',
  categoryId: '',
  clientId: '',
  type: '',
  status: '',
  attachment: '',
  tags: [],
  startDate: '',
  endDate: '',
}

const PAGE_SIZE = 20

/** Despesas pendentes que já passaram do vencimento: é para onde o push de contas atrasadas leva (`?atrasadas=true`). */
function overdueFilters(): Filters {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const pad = (value: number) => String(value).padStart(2, '0')
  const endDate = `${yesterday.getFullYear()}-${pad(yesterday.getMonth() + 1)}-${pad(yesterday.getDate())}`
  return { ...EMPTY_FILTERS, type: 'EXPENSE', status: 'PENDING', endDate }
}

/** Traduz os filtros da tela nos parâmetros da consulta; o que está vazio não vai na URL. */
function toListParams(filters: Filters, page: number): TransactionListParams {
  return {
    page,
    size: PAGE_SIZE,
    accountId: filters.accountId ? Number(filters.accountId) : undefined,
    categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
    clientId: filters.clientId ? Number(filters.clientId) : undefined,
    type: filters.type || undefined,
    status: filters.status || undefined,
    hasAttachment: filters.attachment ? filters.attachment === 'WITH' : undefined,
    tagNames: filters.tags.length > 0 ? filters.tags : undefined,
    startDate: filters.startDate || undefined,
    endDate: filters.endDate || undefined,
  }
}

export function TransactionList() {
  const [searchParams] = useSearchParams()
  const [filters, setFilters] = useState<Filters>(() =>
    searchParams.get('atrasadas') === 'true' ? overdueFilters() : EMPTY_FILTERS,
  )
  const [page, setPage] = useState(0)
  const { data, isLoading, isPlaceholderData } = useTransactions(toListParams(filters, page))
  const transactions = data?.content
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const deleteTransaction = useDeleteTransaction()
  const updateTransactionStatus = useUpdateTransactionStatus()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [attachmentsFor, setAttachmentsFor] = useState<Transaction | null>(null)
  const [tagsFor, setTagsFor] = useState<Transaction | null>(null)

  async function handleDelete(transactionId: number, description: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover a transação "${description}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteTransaction.mutate(transactionId, {
      onSuccess: () => {
        showToast('Transação removida com sucesso.', 'success')
      },
      onError: (error) => {
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a transação.')
      },
    })
  }

  /** Marcar como paga (ou recebida) é definitivo — o backend recusa a volta para pendente —, por isso confirma antes. */
  async function handleMarkAsPaid(transaction: Transaction) {
    const isIncome = transaction.type === 'INCOME'
    const paidWord = isIncome ? 'recebida' : 'paga'
    const confirmed = await confirm({
      title: `Marcar como ${paidWord}`,
      message: `Confirmar que "${transaction.description}" foi ${paidWord}? Depois de confirmada, a transação não poderá voltar para pendente.`,
      confirmLabel: `Marcar como ${paidWord}`,
      variant: 'brand',
    })
    if (!confirmed) return

    updateTransactionStatus.mutate(
      { id: transaction.id, status: 'PAID' },
      {
        onSuccess: () => showToast(`Transação marcada como ${paidWord}.`, 'success'),
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível alterar a situação da transação.'),
      },
    )
  }

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))
  const accountCurrencyById = new Map(accounts?.map((account) => [account.id, account.currency]))
  const categoryNameById = new Map(categories?.map((category) => [category.id, category.name]))
  const clientNameById = new Map(clients?.map((client) => [client.id, client.name]))

  /** Qualquer mudança de filtro volta para a primeira página: a atual pode nem existir no novo resultado. */
  function updateFilters(update: (current: Filters) => Filters) {
    setFilters(update)
    setPage(0)
  }

  // Excluir o último item da última página deixa a página atual sem resultados: volta para a última que existe.
  useEffect(() => {
    if (data && data.totalPages > 0 && page >= data.totalPages) setPage(data.totalPages - 1)
  }, [data, page])

  const activeFiltersCount = Object.values(filters).filter((value) => (Array.isArray(value) ? value.length > 0 : Boolean(value))).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando transações...</p>
  }

  if (!hasActiveFilters && !transactions?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhuma transação lançada ainda. Lance a primeira acima.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      <CollapsibleFilters activeCount={activeFiltersCount}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
          <FormField label="Conta" htmlFor="filter-account">
            <Select
              id="filter-account"
              value={filters.accountId}
              onChange={(e) => updateFilters((f) => ({ ...f, accountId: e.target.value }))}
            >
              <option value="">Todas as contas</option>
              {accounts?.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Categoria" htmlFor="filter-category">
            <Select
              id="filter-category"
              value={filters.categoryId}
              onChange={(e) => updateFilters((f) => ({ ...f, categoryId: e.target.value }))}
            >
              <option value="">Todas as categorias</option>
              {categories?.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Cliente" htmlFor="filter-client">
            <Select
              id="filter-client"
              value={filters.clientId}
              onChange={(e) => updateFilters((f) => ({ ...f, clientId: e.target.value }))}
            >
              <option value="">Todos os clientes</option>
              {clients?.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Tipo" htmlFor="filter-type">
            <Select
              id="filter-type"
              value={filters.type}
              onChange={(e) => updateFilters((f) => ({ ...f, type: e.target.value as TransactionType | '' }))}
            >
              <option value="">Todos</option>
              {Object.entries(TRANSACTION_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Situação" htmlFor="filter-status">
            <Select
              id="filter-status"
              value={filters.status}
              onChange={(e) => updateFilters((f) => ({ ...f, status: e.target.value as TransactionStatus | '' }))}
            >
              <option value="">Todas</option>
              {Object.entries(TRANSACTION_STATUS_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Comprovante" htmlFor="filter-attachment">
            <Select
              id="filter-attachment"
              value={filters.attachment}
              onChange={(e) => updateFilters((f) => ({ ...f, attachment: e.target.value as Filters['attachment'] }))}
            >
              <option value="">Todas</option>
              <option value="WITH">Com comprovante</option>
              <option value="WITHOUT">Sem comprovante</option>
            </Select>
          </FormField>
          <div className="min-w-0 sm:col-span-2">
            <FormField label="Tags (qualquer uma)" htmlFor="filter-tags">
              <TagInput
                id="filter-tags"
                value={filters.tags}
                onChange={(tags) => updateFilters((f) => ({ ...f, tags }))}
                allowCreate={false}
                placeholder="Todas"
              />
            </FormField>
          </div>
          <FormField label="De" htmlFor="filter-start-date">
            <Input
              id="filter-start-date"
              type="date"
              value={filters.startDate}
              onChange={(e) => updateFilters((f) => ({ ...f, startDate: e.target.value }))}
            />
          </FormField>
          <FormField label="Até" htmlFor="filter-end-date">
            <Input
              id="filter-end-date"
              type="date"
              value={filters.endDate}
              onChange={(e) => updateFilters((f) => ({ ...f, endDate: e.target.value }))}
            />
          </FormField>
          {hasActiveFilters && (
            <div className="sm:col-span-2 md:col-span-3 lg:col-span-4">
              <Button variant="secondary" onClick={() => updateFilters(() => EMPTY_FILTERS)}>
                Limpar filtros
              </Button>
            </div>
          )}
        </div>
      </CollapsibleFilters>

      {!transactions?.length ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma transação encontrada com os filtros aplicados.
        </p>
      ) : (
        <div className={`space-y-3 transition-opacity `}>
          {transactions.map((transaction) => (
            <TransactionCard
              key={transaction.id}
              transaction={transaction}
              accountName={accountNameById.get(transaction.accountId) ?? 'conta desconhecida'}
              accountCurrency={accountCurrencyById.get(transaction.accountId)}
              categoryName={transaction.categoryId ? categoryNameById.get(transaction.categoryId) : undefined}
              clientName={transaction.clientId ? clientNameById.get(transaction.clientId) : undefined}
              onDelete={() => handleDelete(transaction.id, transaction.description)}
              isDeleting={deleteTransaction.isPending}
              onMarkAsPaid={() => handleMarkAsPaid(transaction)}
              isMarkingAsPaid={updateTransactionStatus.isPending}
              onOpenAttachments={() => setAttachmentsFor(transaction)}
              onEditTags={() => setTagsFor(transaction)}
            />
          ))}
        </div>
      )}

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          itemsLabel="transações"
          onPageChange={setPage}
          disabled={isPlaceholderData}
        />
      )}

      <Modal
        open={attachmentsFor !== null}
        onClose={() => setAttachmentsFor(null)}
        title={attachmentsFor ? `Comprovantes · ${attachmentsFor.description}` : 'Comprovantes'}
        size="lg"
      >
        {attachmentsFor && <TransactionAttachmentsPanel transactionId={attachmentsFor.id} />}
      </Modal>

      <Modal
        open={tagsFor !== null}
        onClose={() => setTagsFor(null)}
        title={tagsFor ? `Tags · ${tagsFor.description}` : 'Tags'}
      >
        {tagsFor && <TransactionTagsEditor key={tagsFor.id} transaction={tagsFor} onDone={() => setTagsFor(null)} />}
      </Modal>
    </div>
  )
}
