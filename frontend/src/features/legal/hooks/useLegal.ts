import { useEffect } from 'react'
import { CONSENT_REQUIRED_EVENT, authEvents } from '@/shared/auth/authStorage'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { legalApi } from '../api/legalApi'

export const LEGAL_VERSIONS_QUERY_KEY = ['legal', 'versions'] as const
export const CONSENT_QUERY_KEY = ['legal', 'consent'] as const

/** Versões vigentes dos Termos e da Política: o cadastro as envia de volta como prova do aceite. */
export function useLegalVersions() {
  return useQuery({
    queryKey: LEGAL_VERSIONS_QUERY_KEY,
    queryFn: legalApi.getVersions,
    staleTime: 10 * 60 * 1000,
  })
}

/** Situação do aceite da pessoa logada; `pending` liga a tela de aceite obrigatório. */
export function useConsentStatus() {
  return useQuery({
    queryKey: CONSENT_QUERY_KEY,
    queryFn: legalApi.getConsentStatus,
    staleTime: 5 * 60 * 1000,
  })
}

export function useAcceptConsent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: legalApi.acceptConsent,
    onSuccess: (status) => queryClient.setQueryData(CONSENT_QUERY_KEY, status),
  })
}

/**
 * Se o servidor recusar uma chamada por aceite pendente (ex.: as versões mudaram com a pessoa logada),
 * reconsulta a situação para a tela de aceite aparecer na hora.
 */
export function useRefreshConsentWhenRequired() {
  const queryClient = useQueryClient()
  useEffect(() => {
    const refresh = () => void queryClient.invalidateQueries({ queryKey: CONSENT_QUERY_KEY })
    authEvents.addEventListener(CONSENT_REQUIRED_EVENT, refresh)
    return () => authEvents.removeEventListener(CONSENT_REQUIRED_EVENT, refresh)
  }, [queryClient])
}
