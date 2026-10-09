export type ImportFormat = 'CSV' | 'OFX'
export type ImportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

export interface ImportBatch {
  id: number
  accountId: number
  originalFile: string | null
  format: ImportFormat
  importedAt: string
  status: ImportStatus
  transactionCount: number
  uncategorizedCount: number
  /** Linhas do arquivo puladas por já existir uma transação igual (mesma data, hora, descrição e valor). */
  duplicateCount: number
}

export interface ImportBatchUploadInput {
  accountId: number
  file: File
}

export interface TransactionReviewInput {
  categoryId: number | null
  clientId: number | null
}

export const IMPORT_STATUS_LABELS: Record<ImportStatus, string> = {
  PENDING: 'Pendente',
  PROCESSING: 'Processando',
  COMPLETED: 'Concluída',
  FAILED: 'Falhou',
}

export interface ImportCategoryTotal {
  /** `null` no agrupamento "Sem categoria". */
  categoryId: number | null
  name: string
  total: number
  /** Fatia do total de despesas, em %. */
  share: number
}

/** O que a importação trouxe, em números: o primeiro retorno visível do extrato. */
export interface ImportSummary {
  batchId: number
  transactionCount: number
  firstDate: string | null
  lastDate: string | null
  totalIncome: number
  totalExpense: number
  balance: number
  topExpenseCategories: ImportCategoryTotal[]
  uncategorizedCount: number
}
