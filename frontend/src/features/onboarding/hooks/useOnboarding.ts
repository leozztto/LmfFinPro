import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { onboardingApi } from '../api/onboardingApi'

export const ONBOARDING_QUERY_KEY = ['onboarding'] as const

/** O progresso é do usuário, em qualquer grupo. */
export function useOnboarding() {
  return useQuery({
    queryKey: ONBOARDING_QUERY_KEY,
    queryFn: onboardingApi.get,
  })
}

export function useDismissOnboarding() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: onboardingApi.dismiss,
    // Sem devolver a promessa: o React Query esperaria o refetch inteiro antes de chamar o
    // onSuccess de quem chamou o mutate, e é ele que fecha o modal — que ficaria aberto à toa.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ONBOARDING_QUERY_KEY })
    },
  })
}

export function useSetActivationEmails() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: onboardingApi.setActivationEmails,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ONBOARDING_QUERY_KEY }),
  })
}

export function useCompleteOnboardingStep() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: onboardingApi.completeStep,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ONBOARDING_QUERY_KEY }),
  })
}
