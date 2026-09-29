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

export interface RecurringBudget {
  id: number
  categoryId: number
  limitValue: number
  startMonth: string
  endMonth: string | null
  generatedMonths: number
  active: boolean
  /** Calculado no backend; null quando pausado ou encerrado. */
  nextGenerationMonth: string | null
  createdAt: string
}

export interface RecurringBudgetInput {
  categoryId: number
  limitValue: number
  startMonth: string
  endMonth?: string
}

export interface RecurringBudgetUpdateInput {
  limitValue: number
  endMonth?: string
  active: boolean
}
