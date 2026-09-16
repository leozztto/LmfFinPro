export type CategoryType = 'INCOME' | 'EXPENSE'

export interface Category {
  id: number
  name: string
  type: CategoryType
  color: string | null
  icon: string | null
  global: boolean
}

export interface CategoryInput {
  name: string
  type: CategoryType
  color?: string
  icon?: string
}

export const CATEGORY_TYPE_LABELS: Record<CategoryType, string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
}
