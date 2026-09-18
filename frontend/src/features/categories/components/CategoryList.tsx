import { useMemo, useState } from 'react'
import { Button, Card, CollapsibleFilters, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCategories } from '../hooks/useCategories'
import { useDeleteCategory } from '../hooks/useDeleteCategory'
import { CATEGORY_TYPE_LABELS, type CategoryType } from '../types'

interface Filters {
  name: string
  type: CategoryType | ''
  origin: 'own' | 'global' | ''
}

const EMPTY_FILTERS: Filters = { name: '', type: '', origin: '' }

export function CategoryList() {
  const { data: categories, isLoading } = useCategories()
  const deleteCategory = useDeleteCategory()
  const { showToast } = useToast()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)

  function handleDelete(categoryId: number) {
    deleteCategory.mutate(categoryId, {
      onSuccess: () => {
        showToast('Categoria removida com sucesso.', 'success')
      },
      onError: (error) => {
        showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a categoria.')
      },
    })
  }

  const filtered = useMemo(() => {
    if (!categories) return []
    return categories.filter((category) => {
      if (filters.name && !category.name.toLowerCase().includes(filters.name.toLowerCase())) return false
      if (filters.type && category.type !== filters.type) return false
      if (filters.origin === 'own' && category.global) return false
      if (filters.origin === 'global' && !category.global) return false
      return true
    })
  }, [categories, filters])

  const activeFiltersCount = Object.values(filters).filter(Boolean).length
  const hasActiveFilters = activeFiltersCount > 0

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando categorias...</p>
  }

  if (!categories?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Nenhuma categoria cadastrada ainda. Adicione a primeira acima.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      <CollapsibleFilters activeCount={activeFiltersCount}>
        <div className="grid gap-4 sm:grid-cols-3">
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
              onChange={(e) => setFilters((f) => ({ ...f, type: e.target.value as CategoryType | '' }))}
            >
              <option value="">Todos</option>
              {Object.entries(CATEGORY_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Origem" htmlFor="filter-origin">
            <Select
              id="filter-origin"
              value={filters.origin}
              onChange={(e) => setFilters((f) => ({ ...f, origin: e.target.value as Filters['origin'] }))}
            >
              <option value="">Todas</option>
              <option value="own">Minhas categorias</option>
              <option value="global">Padrão do sistema</option>
            </Select>
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

      {filtered.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma categoria encontrada com os filtros aplicados.
        </p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((category) => (
            <Card key={category.id} className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex min-w-0 items-center gap-2">
                <span
                  className="h-3 w-3 shrink-0 rounded-full"
                  style={{ backgroundColor: category.color ?? '#94a3b8' }}
                  aria-hidden
                />
                <div className="min-w-0">
                  <p className="break-words font-medium text-zinc-900 dark:text-zinc-50">{category.name}</p>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    {CATEGORY_TYPE_LABELS[category.type]}
                    {category.global ? ' · padrão do sistema' : ''}
                  </p>
                </div>
              </div>
              {!category.global && (
                <Button
                  variant="secondary"
                  onClick={() => handleDelete(category.id)}
                  disabled={deleteCategory.isPending}
                >
                  Remover
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
