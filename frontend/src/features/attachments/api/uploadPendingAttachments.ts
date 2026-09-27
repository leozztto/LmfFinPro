import { ApiError } from '@/shared/api/httpClient'
import type { PendingAttachment } from '../types'
import { attachmentsApi } from './attachmentsApi'

export interface PendingUploadResult {
  uploadedCount: number
  /** Itens que não foram enviados, com a mensagem do backend em {@code error}. */
  failed: PendingAttachment[]
}

/**
 * Envia os anexos um por vez (cada requisição respeita o limite de 10 MB) e continua mesmo que um
 * deles falhe; o backend valida cada arquivo pelo conteúdo.
 */
export async function uploadPendingAttachments(
  transactionId: number,
  items: PendingAttachment[],
): Promise<PendingUploadResult> {
  let uploadedCount = 0
  const failed: PendingAttachment[] = []
  for (const item of items) {
    try {
      await attachmentsApi.upload(transactionId, item.file, item.documentType)
      uploadedCount++
    } catch (error) {
      failed.push({
        ...item,
        error: error instanceof ApiError ? error.message : 'Não foi possível enviar o arquivo.',
      })
    }
  }
  return { uploadedCount, failed }
}
