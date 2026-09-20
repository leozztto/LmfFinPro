import { useMemo, useState } from 'react'
import { Button, CollapsibleFilters, FormField, IconButton, Input, Modal, Select } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useCategories } from '../hooks/useCategories'
import { useDeleteCategory } from '../hooks/useDeleteCategory'
import { CATEGORY_TYPE_LABELS, type Category, type CategoryType } from '../types'
import { CategoryForm } from './CategoryForm'

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
  const confirm = useConfirm()
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)

  async function handleDelete(categoryId: number, categoryName: string) {
    const confirmed = await confirm({
      message: `Tem certeza que deseja remover a categoria "${categoryName}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return

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
        <div className="grid gap-3 sm:grid-cols-2">
          {filtered.map((category) => (
            <div
              key={category.id}
              className="flex items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-zinc-50 p-4 shadow-sm dark:border-zinc-700 dark:bg-zinc-800"
            >
              <div className="flex min-w-0 flex-1 items-center gap-3">
                <span
                  className="h-3 w-3 shrink-0 rounded-full"
                  style={{ backgroundColor: category.color ?? '#94a3b8' }}
                  aria-hidden
                />
                <div className="min-w-0">
                  <p className="truncate font-medium text-zinc-800 dark:text-zinc-100">{category.name}</p>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400">
                    {CATEGORY_TYPE_LABELS[category.type]}
                    {category.global ? ' · padrão do sistema' : ''}
                  </p>
                </div>
              </div>
              {!category.global && (
                <div className="flex shrink-0 gap-1.5">
                  <IconButton icon={PencilIcon} label="Editar" onClick={() => setEditingCategory(category)} />
                  <IconButton
                    icon={TrashIcon}
                    label="Remover"
                    onClick={() => handleDelete(category.id, category.name)}
                    disabled={deleteCategory.isPending}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <Modal open={editingCategory != null} onClose={() => setEditingCategory(null)} title="Editar categoria">
        {editingCategory && (
          <CategoryForm
            key={editingCategory.id}
            category={editingCategory}
            onSuccess={() => setEditingCategory(null)}
          />
        )}
      </Modal>
    </div>
  )
}
