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
