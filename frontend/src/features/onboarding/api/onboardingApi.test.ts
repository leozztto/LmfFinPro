import { beforeEach, describe, expect, it, vi } from 'vitest'
import { onboardingApi } from './onboardingApi'

const { httpClient } = vi.hoisted(() => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

vi.mock('@/shared/api/httpClient', () => ({ httpClient }))

describe('onboardingApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('reads the progress of the user', () => {
    onboardingApi.get()

    expect(httpClient.get).toHaveBeenCalledWith('/onboarding')
  })

  it('saves a step of the guide as seen', () => {
    onboardingApi.completeStep('ACCOUNT_FILL')

    expect(httpClient.post).toHaveBeenCalledWith('/onboarding/steps/ACCOUNT_FILL', {})
  })

  it('dismisses the guide', () => {
    onboardingApi.dismiss()

    expect(httpClient.post).toHaveBeenCalledWith('/onboarding/dismiss', {})
  })

  it('saves the activation e-mail choice', () => {
    onboardingApi.setActivationEmails(false)

    expect(httpClient.put).toHaveBeenCalledWith('/onboarding/activation-emails', { enabled: false })
  })
})
