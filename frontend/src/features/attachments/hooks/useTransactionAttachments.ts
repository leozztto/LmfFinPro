import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { attachmentsApi } from '../api/attachmentsApi'
import type { AttachmentDocumentType } from '../types'

const attachmentsKey = (transactionId: number) => ['transaction-attachments', transactionId] as const

export function useTransactionAttachments(transactionId: number) {
  return useQuery({
    queryKey: attachmentsKey(transactionId),
    queryFn: () => attachmentsApi.list(transactionId),
  })
}

/** Enviar ou excluir muda a contagem de anexos mostrada na lista de transações. */
function useInvalidateAttachments(transactionId: number) {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: attachmentsKey(transactionId) })
    queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
  }
}

export function useUploadAttachment(transactionId: number) {
  const invalidate = useInvalidateAttachments(transactionId)
  return useMutation({
    mutationFn: ({ file, documentType }: { file: File; documentType: AttachmentDocumentType }) =>
      attachmentsApi.upload(transactionId, file, documentType),
    onSuccess: invalidate,
  })
}

export function useDeleteAttachment(transactionId: number) {
  const invalidate = useInvalidateAttachments(transactionId)
  return useMutation({
    mutationFn: (attachmentId: number) => attachmentsApi.remove(transactionId, attachmentId),
    onSuccess: invalidate,
  })
}
