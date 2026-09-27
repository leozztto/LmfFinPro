export type AttachmentDocumentType = 'PAYMENT_PROOF' | 'INVOICE' | 'RECEIPT' | 'OTHER'

export interface TransactionAttachment {
  id: number
  transactionId: number
  documentType: AttachmentDocumentType
  fileName: string
  contentType: string
  sizeBytes: number
  createdAt: string
}

export const ATTACHMENT_DOCUMENT_TYPE_LABELS: Record<AttachmentDocumentType, string> = {
  PAYMENT_PROOF: 'Comprovante de pagamento',
  INVOICE: 'Nota fiscal',
  RECEIPT: 'Recibo',
  OTHER: 'Outro',
}

/** Arquivo escolhido que ainda não foi enviado; {@code error} guarda o motivo de um envio que falhou. */
export interface PendingAttachment {
  key: number
  file: File
  documentType: AttachmentDocumentType
  error?: string
}
