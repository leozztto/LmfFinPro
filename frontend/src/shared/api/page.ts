/** Resposta das listagens paginadas do backend. `page` começa em 0. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
