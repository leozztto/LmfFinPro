import { useMutation, useQueryClient } from '@tanstack/react-query'
import { TAGS_QUERY_KEY } from '@/features/tags/hooks/useTags'
import { IMPORT_BATCHES_QUERY_KEY } from '@/features/importBatches/hooks/useImportBatches'
import { transactionsApi } from '../api/transactionsApi'
import { TRANSACTIONS_QUERY_KEY } from './useTransactions'

/**
 * Troca só as tags. Tags novas são criadas no backend, por isso a lista de tags recarrega junto; a
 * chave de importações cobre também a tela de revisão do extrato, que usa este mesmo hook.
 */
export function useUpdateTransactionTags() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, tagNames }: { id: number; tagNames: string[] }) => transactionsApi.updateTags(id, tagNames),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: IMPORT_BATCHES_QUERY_KEY })
    },
  })
}
