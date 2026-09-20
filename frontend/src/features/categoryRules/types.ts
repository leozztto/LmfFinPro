export interface CategoryRule {
  id: number
  pattern: string
  categoryId: number
  weight: number
}

export interface CategoryRuleInput {
  pattern: string
  categoryId: number
}
