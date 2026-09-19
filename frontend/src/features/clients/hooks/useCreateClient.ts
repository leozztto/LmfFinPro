import { useMutation, useQueryClient } from '@tanstack/react-query'
import { clientsApi } from '../api/clientsApi'
import { CLIENTS_QUERY_KEY } from './useClients'

export function useCreateClient() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: clientsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CLIENTS_QUERY_KEY }),
  })
}
