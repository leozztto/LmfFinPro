import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, ExpandableText, FormField, Input, Select } from '@/shared/ui'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useTransfers } from '../hooks/useTransfers'
import { useDeleteTransfer } from '../hooks/useDeleteTransfer'

interface Filters {
  accountId: string
  startDate: string
  endDate: string
}

const EMPTY_FILTERS: Filters = { accountId: '', startDate: '', endDate: '' }

export function TransferList() {
  const { data: transfers, isLoading } = useTransfers()
  const { data: accounts } = useAccounts()
  const deleteTransfer = useDeleteTransfer()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))

  const filtered = useMemo(() => {
    if (!transfers) return []
    return transfers.filter((transfer) => {
      if (filters.accountId) {
        const accountId = Number(filters.accountId)
        if (transfer.fromAccountId !== accountId && transfer.toAccountId !== accountId) return false
      }
      if (filters.startDate && transfer.transferDate < filters.startDate) return false
      if (filters.endDate && transfer.transferDate > filters.endDate) return false
      return true
    })
  }, [transfers, filters])

  const sorted = [...filtered].sort((a, b) => b.transferDate.localeCompare(a.transferDate))
  const activeFiltersCount = Object.values(filters).filter(Boolean).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando transferências...</p>
  }

  if (!transfers?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma transferência registrada ainda.</p>
  }

  return (
    <div className="space-y-4">
      <CollapsibleFilters activeCount={activeFiltersCount}>
        <div className="grid gap-4 sm:grid-cols-3">
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
            <div className="sm:col-span-3">
              <Button variant="secondary" onClick={() => setFilters(EMPTY_FILTERS)}>
                Limpar filtros
              </Button>
            </div>
          )}
        </div>
      </CollapsibleFilters>

      {sorted.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma transferência encontrada com os filtros aplicados.
        </p>
      ) : (
        <div className="space-y-3">
          {sorted.map((transfer) => (
            <Card key={transfer.id} className="flex items-center justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="break-words font-medium text-zinc-900 dark:text-zinc-50">
                  {accountNameById.get(transfer.fromAccountId) ?? 'conta desconhecida'} →{' '}
                  {accountNameById.get(transfer.toAccountId) ?? 'conta desconhecida'}
                </p>
                <p className="text-sm text-zinc-500 dark:text-zinc-400">{formatDateOnlyBr(transfer.transferDate)}</p>
                {transfer.description && (
                  <ExpandableText
                    text={transfer.description}
                    className="text-sm text-zinc-500 dark:text-zinc-400"
                  />
                )}
              </div>
              <div className="flex shrink-0 items-center gap-3">
                <span className="font-semibold text-zinc-900 dark:text-zinc-50">
                  {formatCurrency(transfer.amount)}
                </span>
                <Button
                  variant="secondary"
                  onClick={() => deleteTransfer.mutate(transfer.id)}
                  disabled={deleteTransfer.isPending}
                >
                  Remover
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
