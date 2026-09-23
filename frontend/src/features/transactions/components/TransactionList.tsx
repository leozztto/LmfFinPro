import { useMemo, useState } from 'react'
import { Button, CollapsibleFilters, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useTransactions } from '../hooks/useTransactions'
import { useDeleteTransaction } from '../hooks/useDeleteTransaction'
import { TRANSACTION_TYPE_LABELS, type TransactionType } from '../types'
import { TransactionCard } from './TransactionCard'

interface Filters {
  accountId: string
  categoryId: string
  clientId: string
  type: TransactionType | ''
  startDate: string
  endDate: string
}

const EMPTY_FILTERS: Filters = { accountId: '', categoryId: '', clientId: '', type: '', startDate: '', endDate: '' }

export function TransactionList() {
  const { data: transactions, isLoading } = useTransactions()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const deleteTransaction = useDeleteTransaction()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

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

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))
  const categoryNameById = new Map(categories?.map((category) => [category.id, category.name]))
  const clientNameById = new Map(clients?.map((client) => [client.id, client.name]))

  const filtered = useMemo(() => {
    if (!transactions) return []
    return transactions.filter((transaction) => {
      if (filters.accountId && transaction.accountId !== Number(filters.accountId)) return false
      if (filters.categoryId && transaction.categoryId !== Number(filters.categoryId)) return false
      if (filters.clientId && transaction.clientId !== Number(filters.clientId)) return false
      if (filters.type && transaction.type !== filters.type) return false
      if (filters.startDate && transaction.transactionDate < filters.startDate) return false
      if (filters.endDate && transaction.transactionDate > filters.endDate) return false
      return true
    })
  }, [transactions, filters])

  const sorted = [...filtered].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate))
  const activeFiltersCount = Object.values(filters).filter(Boolean).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando transações...</p>
  }

  if (!transactions?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhuma transação lançada ainda. Lance a primeira acima.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      <CollapsibleFilters activeCount={activeFiltersCount}>
        <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <FormField label="Conta" htmlFor="filter-account">
            <Select
              id="filter-account"
              value={filters.accountId}
              onChange={(e) => setFilters((f) => ({ ...f, accountId: e.target.value }))}
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
              onChange={(e) => setFilters((f) => ({ ...f, categoryId: e.target.value }))}
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
              onChange={(e) => setFilters((f) => ({ ...f, clientId: e.target.value }))}
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
              onChange={(e) => setFilters((f) => ({ ...f, type: e.target.value as TransactionType | '' }))}
            >
              <option value="">Todos</option>
              {Object.entries(TRANSACTION_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="De" htmlFor="filter-start-date">
            <Input
              id="filter-start-date"
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters((f) => ({ ...f, startDate: e.target.value }))}
            />
          </FormField>
          <FormField label="Até" htmlFor="filter-end-date">
            <Input
              id="filter-end-date"
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters((f) => ({ ...f, endDate: e.target.value }))}
            />
          </FormField>
          {hasActiveFilters && (
            <div className="sm:col-span-3 lg:col-span-6">
              <Button variant="secondary" onClick={() => setFilters(EMPTY_FILTERS)}>
                Limpar filtros
              </Button>
            </div>
          )}
        </div>
      </CollapsibleFilters>

      {sorted.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma transação encontrada com os filtros aplicados.
        </p>
      ) : (
        <div className="space-y-3">
          {sorted.map((transaction) => (
            <TransactionCard
              key={transaction.id}
              transaction={transaction}
              accountName={accountNameById.get(transaction.accountId) ?? 'conta desconhecida'}
              categoryName={transaction.categoryId ? categoryNameById.get(transaction.categoryId) : undefined}
              clientName={transaction.clientId ? clientNameById.get(transaction.clientId) : undefined}
              onDelete={() => handleDelete(transaction.id, transaction.description)}
              isDeleting={deleteTransaction.isPending}
            />
          ))}
        </div>
      )}
    </div>
  )
}
