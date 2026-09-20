export interface Budget {
  id: number
  categoryId: number
  referenceMonth: string
  limitValue: number
  spentValue: number
}

export interface BudgetInput {
  categoryId: number
  referenceMonth: string
  limitValue: number
}
