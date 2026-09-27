import type { ReportFormat, TagTotalsReportFilters, TransactionReportFilters } from './types'

/**
 * Query string dos relatórios de receitas/despesas: só entram os filtros preenchidos — campo vazio
 * não é enviado, e o backend não aplica o filtro.
 */
export function buildTransactionReportQuery(
  filters: TransactionReportFilters | TagTotalsReportFilters,
  format: ReportFormat,
  tagIds: number[] = [],
): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    const trimmed = value.trim()
    if (trimmed !== '') params.set(key, trimmed)
  }
  // Tags repetidas na URL (tagIds=3&tagIds=7): o backend traz as que têm qualquer uma delas.
  for (const tagId of tagIds) params.append('tagIds', String(tagId))
  params.set('format', format)
  return params.toString()
}

/** Mensagem de erro dos filtros, ou null se estiverem coerentes (o backend valida de novo). */
export function validateTransactionReportFilters(filters: TransactionReportFilters): string | null {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    return 'A data inicial deve ser anterior ou igual à data final.'
  }
  const min = filters.minAmount.trim() === '' ? null : Number(filters.minAmount)
  const max = filters.maxAmount.trim() === '' ? null : Number(filters.maxAmount)
  if ((min !== null && (Number.isNaN(min) || min < 0)) || (max !== null && (Number.isNaN(max) || max < 0))) {
    return 'Os valores mínimo e máximo devem ser números positivos.'
  }
  if (min !== null && max !== null && min > max) {
    return 'O valor mínimo deve ser menor ou igual ao valor máximo.'
  }
  return null
}
