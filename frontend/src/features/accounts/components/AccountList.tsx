import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '../hooks/useAccounts'
import { useDeleteAccount } from '../hooks/useDeleteAccount'
import { ACCOUNT_TYPE_LABELS, type AccountType } from '../types'
import { formatCurrency } from '@/shared/format/currency'

interface Filters {
  name: string
  type: AccountType | ''
}

const EMPTY_FILTERS: Filters = { name: '', type: '' }

export function AccountList() {
  const { data: accounts, isLoading } = useAccounts()
  const deleteAccount = useDeleteAccount()
  const { showToast } = useToast()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

  function handleDelete(accountId: number) {
    deleteAccount.mutate(accountId, {
      onError: (error) => {
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a conta.')
      },
    })
  }

  const filtered = useMemo(() => {
    if (!accounts) return []
    return accounts.filter((account) => {
      if (filters.name && !account.name.toLowerCase().includes(filters.name.toLowerCase())) return false
      if (filters.type && account.type !== filters.type) return false
      return true
    })
  }, [accounts, filters])

  const activeFiltersCount = Object.values(filters).filter(Boolean).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando contas...</p>
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhuma conta cadastrada ainda. Adicione a primeira acima.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      <CollapsibleFilters activeCount={activeFiltersCount}>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Nome" htmlFor="filter-name">
            <Input
              id="filter-name"
              placeholder="Buscar por nome"
              value={filters.name}
              onChange={(e) => setFilters((f) => ({ ...f, name: e.target.value }))}
            />
          </FormField>
          <FormField label="Tipo" htmlFor="filter-type">
            <Select
              id="filter-type"
              value={filters.type}
              onChange={(e) => setFilters((f) => ({ ...f, type: e.target.value as AccountType | '' }))}
            >
              <option value="">Todos</option>
              {Object.entries(ACCOUNT_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          {hasActiveFilters && (
            <div className="sm:col-span-2">
              <Button variant="secondary" onClick={() => setFilters(EMPTY_FILTERS)}>
                Limpar filtros
              </Button>
            </div>
          )}
        </div>
      </CollapsibleFilters>

      {filtered.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma conta encontrada com os filtros aplicados.
        </p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {filtered.map((account) => (
            <Card key={account.id} className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="break-words font-medium text-zinc-900 dark:text-zinc-50">{account.name}</p>
                <p className="text-sm text-zinc-500 dark:text-zinc-400">{ACCOUNT_TYPE_LABELS[account.type]}</p>
                <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">
                  Saldo inicial: {formatCurrency(account.initialBalance)}
                </p>
                <p className="text-sm font-medium text-zinc-900 dark:text-zinc-50">
                  Saldo atual: {formatCurrency(account.currentBalance)}
                </p>
              </div>
              <Button
                variant="secondary"
                onClick={() => handleDelete(account.id)}
                disabled={deleteAccount.isPending}
              >
                Remover
              </Button>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
