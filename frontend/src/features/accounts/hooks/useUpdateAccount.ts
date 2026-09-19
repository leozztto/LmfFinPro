import { useMutation, useQueryClient } from '@tanstack/react-query'
import { accountsApi } from '../api/accountsApi'
import { ACCOUNTS_QUERY_KEY } from './useAccounts'
import type { AccountInput } from '../types'

export function useUpdateAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: AccountInput }) => accountsApi.update(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY }),
  })
}
