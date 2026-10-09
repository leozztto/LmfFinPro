// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { OnboardingModal } from './OnboardingModal'
import { ONBOARDING_QUERY_KEY } from '../hooks/useOnboarding'
import { ONBOARDING_PARAM, TOUR_STEPS } from '../steps'
import type { OnboardingProgress, OnboardingStepId } from '../types'

const { onboardingApi } = vi.hoisted(() => ({
  onboardingApi: {
    get: vi.fn(),
    completeStep: vi.fn(),
    dismiss: vi.fn(),
    setActivationEmails: vi.fn(),
  },
}))

vi.mock('../api/onboardingApi', () => ({ onboardingApi }))

/** Estado do servidor em que só os primeiros {@code seen} passos do guia foram vistos. */
function progress(seen: number): OnboardingProgress {
  const steps = TOUR_STEPS.map((step, index) => ({ id: step.id, done: index < seen }))
  return {
    steps,
    completedCount: seen,
    totalCount: steps.length,
    completed: seen === steps.length,
    dismissed: false,
    activationEmailsEnabled: true,
  }
}

function renderModal(path = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <OnboardingModal />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return queryClient
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('OnboardingModal tour', () => {
  it('starts at the first step with the highlighted preview and nothing to create', async () => {
    onboardingApi.get.mockResolvedValue(progress(0))

    renderModal()

    expect(await screen.findByText('Passo 1 de 14')).toBeTruthy()
    expect(screen.getByText(TOUR_STEPS[0].title)).toBeTruthy()
    expect(screen.getByTestId('tour-preview-ACCOUNT_OPEN')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Voltar' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('resumes from the first step not seen yet', async () => {
    onboardingApi.get.mockResolvedValue(progress(5))

    renderModal()

    expect(await screen.findByText('Passo 6 de 14')).toBeTruthy()
    expect(screen.getByTestId('tour-preview-IMPORT_PICK')).toBeTruthy()
    expect(screen.getByRole('button', { name: /Baixar modelo de CSV/ })).toBeTruthy()
  })

  it('saves the step on "Próximo" and moves to the next one', async () => {
    onboardingApi.get.mockResolvedValue(progress(0))
    onboardingApi.completeStep.mockResolvedValue(undefined)

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Próximo' }))

    expect(await screen.findByText('Passo 2 de 14')).toBeTruthy()
    expect(onboardingApi.completeStep.mock.calls[0]?.[0]).toBe<OnboardingStepId>('ACCOUNT_OPEN')
  })

  it('goes back without saving anything', async () => {
    onboardingApi.get.mockResolvedValue(progress(2))

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Voltar' }))

    expect(await screen.findByText('Passo 2 de 14')).toBeTruthy()
    expect(onboardingApi.completeStep).not.toHaveBeenCalled()
  })

  it('does not save a step that was already seen', async () => {
    onboardingApi.get.mockResolvedValue(progress(2))

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Voltar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Próximo' }))

    expect(await screen.findByText('Passo 3 de 14')).toBeTruthy()
    expect(onboardingApi.completeStep).not.toHaveBeenCalled()
  })

  it('finishes on the last step and closes', async () => {
    onboardingApi.get.mockResolvedValue(progress(TOUR_STEPS.length - 1))
    onboardingApi.completeStep.mockResolvedValue(undefined)

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Concluir' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(onboardingApi.completeStep.mock.calls[0]?.[0]).toBe<OnboardingStepId>('REPORTS_VIEW')
  })

  it('stays on the step and warns when the progress cannot be saved', async () => {
    onboardingApi.get.mockResolvedValue(progress(0))
    onboardingApi.completeStep.mockRejectedValue(new Error('falhou'))

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Próximo' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Não foi possível salvar')
    expect(screen.getByText('Passo 1 de 14')).toBeTruthy()
  })

  it('explains when the progress cannot be loaded', async () => {
    onboardingApi.get.mockRejectedValue(new Error('falhou'))

    renderModal(`/?${ONBOARDING_PARAM}=1`)

    expect((await screen.findByRole('alert')).textContent).toContain('Não foi possível carregar')
  })
})

describe('OnboardingModal visibility', () => {
  it('opens by itself over any screen while the guide is not finished', async () => {
    onboardingApi.get.mockResolvedValue(progress(2))

    renderModal('/transacoes')

    expect(await screen.findByRole('dialog')).toBeTruthy()
  })

  it('stays closed once every step was seen', async () => {
    onboardingApi.get.mockResolvedValue(progress(TOUR_STEPS.length))

    renderModal('/')

    await waitFor(() => expect(onboardingApi.get).toHaveBeenCalled())
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('stays closed after the guide was dismissed for good', async () => {
    onboardingApi.get.mockResolvedValue({ ...progress(0), dismissed: true })

    renderModal('/')

    await waitFor(() => expect(onboardingApi.get).toHaveBeenCalled())
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('can be opened again to review when everything was seen', async () => {
    onboardingApi.get.mockResolvedValue(progress(TOUR_STEPS.length))

    renderModal(`/?${ONBOARDING_PARAM}=1`)

    expect(await screen.findByText('Passo 1 de 14')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Não mostrar mais' })).toBeNull()
  })

  it('closes on "Continuar depois", stays closed while the screen is open and comes back on the next visit', async () => {
    onboardingApi.get.mockResolvedValue(progress(2))

    const queryClient = renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Continuar depois' }))
    expect(screen.queryByRole('dialog')).toBeNull()

    await act(async () => {
      await queryClient.invalidateQueries({ queryKey: ONBOARDING_QUERY_KEY })
    })
    expect(screen.queryByRole('dialog')).toBeNull()

    cleanup()
    renderModal('/')
    expect(await screen.findByText('Passo 3 de 14')).toBeTruthy()
  })

  it('dismisses for good through "Não mostrar mais"', async () => {
    onboardingApi.get.mockResolvedValue(progress(0))
    onboardingApi.dismiss.mockResolvedValue(undefined)

    renderModal()
    fireEvent.click(await screen.findByRole('button', { name: 'Não mostrar mais' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(onboardingApi.dismiss).toHaveBeenCalled()
  })
})
