import { useQuery } from '@tanstack/react-query'
import { statusApi } from '../api/statusApi'

const REFRESH_INTERVAL_MS = 60 * 1000

/** Situação da plataforma, reconsultada a cada minuto enquanto a página de suporte está aberta. */
export function usePlatformStatus() {
  return useQuery({
    queryKey: ['support', 'status'],
    queryFn: statusApi.getStatus,
    refetchInterval: REFRESH_INTERVAL_MS,
    retry: false,
  })
}
