import { httpClient } from '@/shared/api/httpClient'
import type { AttachmentDocumentType, TransactionAttachment } from '../types'

const base = (transactionId: number) => `/transactions/${transactionId}/attachments`

export const attachmentsApi = {
  list: (transactionId: number) => httpClient.get<TransactionAttachment[]>(base(transactionId)),
  upload: (transactionId: number, file: File, documentType: AttachmentDocumentType) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('documentType', documentType)
    return httpClient.postForm<TransactionAttachment>(base(transactionId), formData)
  },
  content: (transactionId: number, attachmentId: number, download = false) =>
    httpClient.getBlob(`${base(transactionId)}/${attachmentId}/content${download ? '?download=true' : ''}`),
  remove: (transactionId: number, attachmentId: number) =>
    httpClient.delete(`${base(transactionId)}/${attachmentId}`),
}
