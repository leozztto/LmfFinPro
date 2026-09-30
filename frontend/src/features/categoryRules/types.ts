export interface CategoryRule {
  id: number
  pattern: string
  categoryId: number
  weight: number
  global: boolean
}

export interface CategoryRuleInput {
  pattern: string
  categoryId: number
}
