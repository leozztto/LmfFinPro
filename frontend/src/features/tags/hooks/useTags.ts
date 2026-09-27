import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { RECURRING_TRANSACTIONS_QUERY_KEY } from '@/features/recurringTransactions/hooks/useRecurringTransactions'
import { tagsApi } from '../api/tagsApi'
import type { TagInput } from '../types'

export const TAGS_QUERY_KEY = ['tags'] as const

export function useTags() {
  return useQuery({ queryKey: TAGS_QUERY_KEY, queryFn: tagsApi.list })
}

/**
 * Renomear, recolorir ou excluir uma tag muda como ela aparece nas transações e recorrências,
 * então essas listas são recarregadas junto.
 */
function useInvalidateTagged() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY })
    queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
    queryClient.invalidateQueries({ queryKey: RECURRING_TRANSACTIONS_QUERY_KEY })
  }
}

export function useCreateTag() {
  const invalidate = useInvalidateTagged()
  return useMutation({ mutationFn: (input: TagInput) => tagsApi.create(input), onSuccess: invalidate })
}

export function useUpdateTag() {
  const invalidate = useInvalidateTagged()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: TagInput }) => tagsApi.update(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteTag() {
  const invalidate = useInvalidateTagged()
  return useMutation({ mutationFn: (id: number) => tagsApi.remove(id), onSuccess: invalidate })
}
