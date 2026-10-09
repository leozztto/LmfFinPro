import { httpClient } from '@/shared/api/httpClient'
import type { OnboardingProgress, OnboardingStepId } from '../types'

export const onboardingApi = {
  get: () => httpClient.get<OnboardingProgress>('/onboarding'),
  completeStep: (step: OnboardingStepId) => httpClient.post<void>(`/onboarding/steps/${step}`, {}),
  dismiss: () => httpClient.post<void>('/onboarding/dismiss', {}),
  setActivationEmails: (enabled: boolean) =>
    httpClient.put<void, { enabled: boolean }>('/onboarding/activation-emails', { enabled }),
}
