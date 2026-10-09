// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ActivationEmailsCard } from './ActivationEmailsCard'

const { onboardingApi } = vi.hoisted(() => ({
  onboardingApi: {
    get: vi.fn(),
    dismiss: vi.fn(),
    setActivationEmails: vi.fn(),
  },
}))

vi.mock('../api/onboardingApi', () => ({ onboardingApi }))
vi.mock('@/shared/toast/ToastContext', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}))

describe('ActivationEmailsCard', () => {
  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('lets the user opt out of the first-steps e-mails', async () => {
    onboardingApi.get.mockResolvedValue({
      steps: [],
      completedCount: 0,
      totalCount: 3,
      completed: false,
      dismissed: false,
      activationEmailsEnabled: true,
    })
    onboardingApi.setActivationEmails.mockResolvedValue(undefined)

    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ActivationEmailsCard />
      </QueryClientProvider>,
    )

    const checkbox = (await screen.findByRole('checkbox', {
      name: 'Quero receber os e-mails de primeiros passos',
    })) as HTMLInputElement
    expect(checkbox.checked).toBe(true)

    fireEvent.click(checkbox)

    await waitFor(() => expect(onboardingApi.setActivationEmails.mock.calls[0]?.[0]).toBe(false))
  })
})
