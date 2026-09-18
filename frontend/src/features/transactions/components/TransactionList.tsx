import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, ExpandableText, FormField, Input, Select } from '@/shared/ui'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useTransactions } from '../hooks/useTransactions'
import { useDeleteTransaction } from '../hooks/useDeleteTransaction'
import { TRANSACTION_TYPE_LABELS, type TransactionType } from '../types'

interface Filters {
  accountId: string
  categoryId: string
  type: TransactionType | ''
  startDate: string
  endDate: string
}

const EMPTY_FILTERS: Filters = { accountId: '', categoryId: '', type: '', startDate: '', endDate: '' }

export function TransactionList() {
  const { data: transactions, isLoading } = useTransactions()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const deleteTransaction = useDeleteTransaction()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))
  const categoryNameById = new Map(categories?.map((category) => [category.id, category.name]))

  const filtered = useMemo(() => {
    if (!transactions) return []
    return transactions.filter((transaction) => {
      if (filters.accountId && transaction.accountId !== Number(filters.accountId)) return false
      if (filters.categoryId && transaction.categoryId !== Number(filters.categoryId)) return false
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
        <div className="grid gap-4 sm:grid-cols-5">
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
            <div className="sm:col-span-5">
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
          {sorted.map((transaction) => {
            const isTransfer = transaction.transferId != null
            return (
              <Card key={transaction.id} className="flex items-center justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <ExpandableText
                      text={transaction.description}
                      className="font-medium text-zinc-900 dark:text-zinc-50"
                    />
                    {isTransfer && (
                      <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200">
                        Transferência
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    {formatDateOnlyBr(transaction.transactionDate)} ·{' '}
                    {accountNameById.get(transaction.accountId) ?? 'conta desconhecida'}
                    {transaction.categoryId ? ` · ${categoryNameById.get(transaction.categoryId) ?? ''}` : ''}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <span
                    className={
                      transaction.type === 'INCOME'
                        ? 'font-semibold text-green-600 dark:text-green-500'
                        : 'font-semibold text-red-600'
                    }
                  >
                    {transaction.type === 'INCOME' ? '+' : '-'} {formatCurrency(transaction.amount)}
                  </span>
                  <Button
                    variant="secondary"
                    onClick={() => deleteTransaction.mutate(transaction.id)}
                    disabled={deleteTransaction.isPending || isTransfer}
                    title={
                      isTransfer
                        ? 'Esta transação faz parte de uma transferência. Exclua-a na tela de Transferências.'
                        : undefined
                    }
                  >
                    Remover
                  </Button>
                </div>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}
