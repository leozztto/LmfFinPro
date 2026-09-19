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
