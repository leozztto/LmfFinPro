import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { attachmentsApi } from '../api/attachmentsApi'
import { uploadPendingAttachments } from '../api/uploadPendingAttachments'
import type { PendingAttachment } from '../types'

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

/**
 * Envia a fila de anexos de uma transação. O id vem na chamada porque, na criação, a transação só
 * passa a existir depois que o formulário é salvo.
 */
export function useUploadPendingAttachments() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ transactionId, items }: { transactionId: number; items: PendingAttachment[] }) =>
      uploadPendingAttachments(transactionId, items),
    onSettled: (_result, _error, { transactionId }) => {
      queryClient.invalidateQueries({ queryKey: attachmentsKey(transactionId) })
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
    },
  })
}

export function useDeleteAttachment(transactionId: number) {
  const invalidate = useInvalidateAttachments(transactionId)
  return useMutation({
    mutationFn: (attachmentId: number) => attachmentsApi.remove(transactionId, attachmentId),
    onSuccess: invalidate,
  })
}
