import { useMutation } from '@tanstack/react-query'
import { cepApi } from '../api/cepApi'

export function useCepLookup() {
  return useMutation({
    mutationFn: cepApi.lookup,
  })
}
