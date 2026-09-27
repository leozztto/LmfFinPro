import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, FormField, IconButton, Input, Modal, Select } from '@/shared/ui'
import { PencilIcon, TrashIcon, TrendingUpIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useAccounts } from '../hooks/useAccounts'
import { useDeleteAccount } from '../hooks/useDeleteAccount'
import { ACCOUNT_SCOPE_LABELS, ACCOUNT_TYPE_LABELS, type Account, type AccountType } from '../types'
import { formatCurrency } from '@/shared/format/currency'
import { AccountForm } from './AccountForm'
import { AccountValuationsPanel } from './AccountValuationsPanel'

interface Filters {
  name: string
  type: AccountType | ''
}

const EMPTY_FILTERS: Filters = { name: '', type: '' }

export function AccountList() {
  const { data: accounts, isLoading } = useAccounts()
  const deleteAccount = useDeleteAccount()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)
  const [editingAccount, setEditingAccount] = useState<Account | null>(null)
  const [valuingAccount, setValuingAccount] = useState<Account | null>(null)

  async function handleDelete(accountId: number, accountName: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover a conta "${accountName}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteAccount.mutate(accountId, {
      onSuccess: () => {
        showToast('Conta removida com sucesso.', 'success')
      },
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
            <Card key={account.id} className="flex items-end justify-between gap-3">
              <div className="min-w-0">
                <p className="break-words font-medium text-zinc-800 dark:text-zinc-100">{account.name}</p>
                <p className="flex flex-wrap items-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
                  {ACCOUNT_TYPE_LABELS[account.type]}
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      account.scope === 'BUSINESS'
                        ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-500/15 dark:text-indigo-300'
                        : 'bg-zinc-100 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300'
                    }`}
                  >
                    {ACCOUNT_SCOPE_LABELS[account.scope]}
                  </span>
                  {account.currency !== 'BRL' && (
                    <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-500/15 dark:text-amber-300">
                      {account.currency}
                    </span>
                  )}
                </p>
                <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">
                  Saldo inicial: {formatCurrency(account.initialBalance, account.currency)}
                </p>
                <p className="text-sm font-medium text-zinc-800 dark:text-zinc-100">
                  Saldo atual: {formatCurrency(account.currentBalance, account.currency)}
                </p>
                {account.currency !== 'BRL' && account.currentBalanceInBrl != null && (
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    ≈ {formatCurrency(account.currentBalanceInBrl)} pela última cotação
                  </p>
                )}
              </div>
              <div className="flex shrink-0 gap-1.5">
                {account.type === 'INVESTMENT' && (
                  <IconButton
                    icon={TrendingUpIcon}
                    label="Valor de mercado"
                    onClick={() => setValuingAccount(account)}
                  />
                )}
                <IconButton icon={PencilIcon} label="Editar" onClick={() => setEditingAccount(account)} />
                <IconButton
                  icon={TrashIcon}
                  label="Remover"
                  onClick={() => handleDelete(account.id, account.name)}
                  disabled={deleteAccount.isPending}
                />
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={editingAccount != null} onClose={() => setEditingAccount(null)} title="Editar conta">
        {editingAccount && (
          <AccountForm key={editingAccount.id} account={editingAccount} onSuccess={() => setEditingAccount(null)} />
        )}
      </Modal>

      <Modal
        open={valuingAccount != null}
        onClose={() => setValuingAccount(null)}
        title={valuingAccount ? `Valor de mercado · ${valuingAccount.name}` : 'Valor de mercado'}
      >
        {valuingAccount && <AccountValuationsPanel key={valuingAccount.id} accountId={valuingAccount.id} />}
      </Modal>
    </div>
  )
}
