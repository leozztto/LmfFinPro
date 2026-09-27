/** Tag como vem dentro de uma transação ou recorrência. `name` sem o "#", já normalizado. */
export interface TagSummary {
  id: number
  name: string
  color: string | null
}

/** Tag na tela de gestão, com quantas transações a usam. */
export interface Tag extends TagSummary {
  transactionCount: number
}

export interface TagInput {
  name: string
  color?: string | null
}
