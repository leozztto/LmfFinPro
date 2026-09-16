import { Button, Card } from '@/shared/ui'
import { useCategories } from '../hooks/useCategories'
import { useDeleteCategory } from '../hooks/useDeleteCategory'
import { CATEGORY_TYPE_LABELS } from '../types'

export function CategoryList() {
  const { data: categories, isLoading } = useCategories()
  const deleteCategory = useDeleteCategory()

  if (isLoading) {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando categorias...</p>
  }

  if (!categories?.length) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">
        Nenhuma categoria cadastrada ainda. Adicione a primeira acima.
      </p>
    )
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {categories.map((category) => (
        <Card key={category.id} className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span
              className="h-3 w-3 rounded-full"
              style={{ backgroundColor: category.color ?? '#94a3b8' }}
              aria-hidden
            />
            <div>
              <p className="font-medium text-slate-900 dark:text-slate-50">{category.name}</p>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                {CATEGORY_TYPE_LABELS[category.type]}
                {category.global ? ' · padrão do sistema' : ''}
              </p>
            </div>
          </div>
          {!category.global && (
            <Button
              variant="secondary"
              onClick={() => deleteCategory.mutate(category.id)}
              disabled={deleteCategory.isPending}
            >
              Remover
            </Button>
          )}
        </Card>
      ))}
    </div>
  )
}
