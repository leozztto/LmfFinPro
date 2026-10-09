// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ONBOARDING_QUERY_KEY,
  useCompleteOnboardingStep,
  useDismissOnboarding,
  useOnboarding,
  useSetActivationEmails,
} from './useOnboarding'

const { onboardingApi } = vi.hoisted(() => ({
  onboardingApi: {
    get: vi.fn(),
    completeStep: vi.fn(),
    dismiss: vi.fn(),
    setActivationEmails: vi.fn(),
  },
}))

vi.mock('../api/onboardingApi', () => ({ onboardingApi }))

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { wrapper, invalidate }
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('useOnboarding', () => {
  it('loads the progress of the user under the onboarding key', async () => {
    const progress = { steps: [], completedCount: 0, totalCount: 14, completed: false, dismissed: false }
    onboardingApi.get.mockResolvedValue(progress)
    const { wrapper } = setup()

    const { result } = renderHook(() => useOnboarding(), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(progress))
    expect(ONBOARDING_QUERY_KEY).toEqual(['onboarding'])
  })

  it('reports the failure instead of throwing', async () => {
    onboardingApi.get.mockRejectedValue(new Error('falhou'))
    const { wrapper } = setup()

    const { result } = renderHook(() => useOnboarding(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('onboarding mutations', () => {
  it('saves a step and refreshes the progress', async () => {
    onboardingApi.completeStep.mockResolvedValue(undefined)
    const { wrapper, invalidate } = setup()
    const { result } = renderHook(() => useCompleteOnboardingStep(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync('CALENDAR_VIEW')
    })

    expect(onboardingApi.completeStep.mock.calls[0]?.[0]).toBe('CALENDAR_VIEW')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ONBOARDING_QUERY_KEY })
  })

  it('dismisses the guide and refreshes the progress', async () => {
    onboardingApi.dismiss.mockResolvedValue(undefined)
    const { wrapper, invalidate } = setup()
    const { result } = renderHook(() => useDismissOnboarding(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync()
    })

    expect(onboardingApi.dismiss).toHaveBeenCalled()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ONBOARDING_QUERY_KEY })
  })

  it('saves the e-mail choice and refreshes the progress', async () => {
    onboardingApi.setActivationEmails.mockResolvedValue(undefined)
    const { wrapper, invalidate } = setup()
    const { result } = renderHook(() => useSetActivationEmails(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync(false)
    })

    expect(onboardingApi.setActivationEmails.mock.calls[0]?.[0]).toBe(false)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ONBOARDING_QUERY_KEY })
  })

  it('does not refresh when saving fails', async () => {
    onboardingApi.completeStep.mockRejectedValue(new Error('falhou'))
    const { wrapper, invalidate } = setup()
    const { result } = renderHook(() => useCompleteOnboardingStep(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync('ACCOUNT_OPEN').catch(() => undefined)
    })

    expect(invalidate).not.toHaveBeenCalled()
  })
})
