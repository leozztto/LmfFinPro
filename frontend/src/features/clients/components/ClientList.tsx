import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, FormField, Input, Modal, Select } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useClients } from '../hooks/useClients'
import { useDeleteClient } from '../hooks/useDeleteClient'
import { CLIENT_WORK_TYPE_LABELS, type Client, type ClientWorkType } from '../types'
import { ClientForm } from './ClientForm'
import { ClientDetails } from './ClientDetails'

interface Filters {
  name: string
  status: 'active' | 'inactive' | ''
  workType: ClientWorkType | ''
}

const EMPTY_FILTERS: Filters = { name: '', status: '', workType: '' }

export function ClientList() {
  const { data: clients, isLoading } = useClients()
  const deleteClient = useDeleteClient()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)
  const [editingClient, setEditingClient] = useState<Client | null>(null)
  const [viewingClient, setViewingClient] = useState<Client | null>(null)

  async function handleDelete(clientId: number, clientName: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover o cliente "${clientName}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

    deleteClient.mutate(clientId, {
      onSuccess: () => {
        showToast('Cliente removido com sucesso.', 'success')
      },
      onError: (error) => {
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover o cliente.')
      },
    })
  }

  const filtered = useMemo(() => {
    if (!clients) return []
    return clients.filter((client) => {
      if (filters.name && !client.name.toLowerCase().includes(filters.name.toLowerCase())) return false
      if (filters.status === 'active' && !client.active) return false
      if (filters.status === 'inactive' && client.active) return false
      if (filters.workType && client.workType !== filters.workType) return false
      return true
    })
  }, [clients, filters])

  const activeFiltersCount = Object.values(filters).filter(Boolean).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando clientes...</p>
  }

  if (!clients?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhum cliente cadastrado ainda. Adicione o primeiro acima.
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
          <FormField label="Status" htmlFor="filter-status">
            <Select
              id="filter-status"
              value={filters.status}
              onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value as Filters['status'] }))}
            >
              <option value="">Todos</option>
              <option value="active">Ativos</option>
              <option value="inactive">Inativos</option>
            </Select>
          </FormField>
          <FormField label="Tipo de trabalho" htmlFor="filter-work-type">
            <Select
              id="filter-work-type"
              value={filters.workType}
              onChange={(e) => setFilters((f) => ({ ...f, workType: e.target.value as Filters['workType'] }))}
            >
              <option value="">Todos</option>
              {Object.entries(CLIENT_WORK_TYPE_LABELS).map(([value, label]) => (
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
          Nenhum cliente encontrado com os filtros aplicados.
        </p>
      ) : (
        <div className="space-y-3">
          {filtered.map((client) => (
            <Card
              key={client.id}
              className="flex cursor-pointer items-center justify-between gap-3 hover:border-zinc-300 dark:hover:border-zinc-600"
              onClick={() => setViewingClient(client)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  setViewingClient(client)
                }
              }}
            >
              <div className="flex min-w-0 items-center gap-2">
                <span
                  className="h-3 w-3 shrink-0 rounded-full"
                  style={{ backgroundColor: client.color ?? '#94a3b8' }}
                  aria-hidden
                />
                <p className="min-w-0 truncate font-medium text-zinc-800 dark:text-zinc-100">{client.name}</p>
              </div>
              <div className="flex shrink-0 gap-2" onClick={(e) => e.stopPropagation()}>
                <Button
                  variant="secondary"
                  onClick={() => setEditingClient(client)}
                  aria-label="Editar"
                  title="Editar"
                  className="px-3"
                >
                  <PencilIcon />
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => handleDelete(client.id, client.name)}
                  disabled={deleteClient.isPending}
                  aria-label="Remover"
                  title="Remover"
                  className="px-3"
                >
                  <TrashIcon />
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={editingClient != null} onClose={() => setEditingClient(null)} title="Editar cliente">
        {editingClient && (
          <ClientForm
            key={editingClient.id}
            client={editingClient}
            onSuccess={() => setEditingClient(null)}
          />
        )}
      </Modal>

      <Modal open={viewingClient != null} onClose={() => setViewingClient(null)} title="Detalhes do cliente">
        {viewingClient && <ClientDetails client={viewingClient} />}
      </Modal>
    </div>
  )
}
