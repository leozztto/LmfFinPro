import { useMemo, useState } from 'react'
import { Button, CollapsibleFilters, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useTransfers } from '../hooks/useTransfers'
import { useDeleteTransfer } from '../hooks/useDeleteTransfer'
import { TransferCard } from './TransferCard'

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
  const { showToast } = useToast()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

  function handleDelete(transferId: number) {
    deleteTransfer.mutate(transferId, {
      onSuccess: () => {
        showToast('Transferência removida com sucesso.', 'success')
      },
      onError: (error) => {
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a transferência.')
      },
    })
  }

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
            <TransferCard
              key={transfer.id}
              transfer={transfer}
              fromAccountName={accountNameById.get(transfer.fromAccountId) ?? 'conta desconhecida'}
              toAccountName={accountNameById.get(transfer.toAccountId) ?? 'conta desconhecida'}
              onDelete={() => handleDelete(transfer.id)}
              isDeleting={deleteTransfer.isPending}
            />
          ))}
        </div>
      )}
    </div>
  )
}
