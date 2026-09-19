import { useMutation, useQueryClient } from '@tanstack/react-query'
import { clientsApi } from '../api/clientsApi'
import { CLIENTS_QUERY_KEY } from './useClients'
import type { ClientInput } from '../types'

export function useUpdateClient() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ClientInput }) => clientsApi.update(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CLIENTS_QUERY_KEY }),
  })
}
