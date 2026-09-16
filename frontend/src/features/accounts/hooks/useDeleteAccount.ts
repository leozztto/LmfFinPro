import { useMutation, useQueryClient } from '@tanstack/react-query'
import { accountsApi } from '../api/accountsApi'
import { ACCOUNTS_QUERY_KEY } from './useAccounts'

export function useDeleteAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: accountsApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY }),
  })
}
