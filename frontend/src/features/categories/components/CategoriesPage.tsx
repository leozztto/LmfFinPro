import { CategoryForm } from './CategoryForm'
import { CategoryList } from './CategoryList'

export function CategoriesPage() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">Categorias</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Categorias padrão do sistema e as suas próprias, usadas para classificar transações.
        </p>
      </div>
      <CategoryForm />
      <CategoryList />
    </div>
  )
}
