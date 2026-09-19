import { useMutation, useQueryClient } from '@tanstack/react-query'
import { clientsApi } from '../api/clientsApi'
import { CLIENTS_QUERY_KEY } from './useClients'

export function useDeleteClient() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: clientsApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CLIENTS_QUERY_KEY }),
  })
}
