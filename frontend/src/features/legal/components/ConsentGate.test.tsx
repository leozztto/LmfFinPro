// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ConsentStatus } from '../types'
import { ConsentGate } from './ConsentGate'

const state = vi.hoisted(() => ({
  status: { data: undefined, isLoading: false } as { data?: unknown; isLoading: boolean },
  accept: { mutate: vi.fn(), isPending: false, isError: false, error: null as unknown },
  logout: vi.fn(),
}))

vi.mock('../hooks/useLegal', () => ({
  useConsentStatus: () => state.status,
  useAcceptConsent: () => state.accept,
  useRefreshConsentWhenRequired: () => undefined,
}))
vi.mock('@/shared/auth/AuthContext', () => ({ useAuth: () => ({ logout: state.logout }) }))
vi.mock('@/shared/theme/ThemeToggle', () => ({ ThemeToggle: () => null }))

function status(overrides: Partial<ConsentStatus> = {}): ConsentStatus {
  return {
    terms: { currentVersion: '2026-10-07', acceptedVersion: null, acceptedAt: null, accepted: false },
    privacy: { currentVersion: '2026-10-07', acceptedVersion: null, acceptedAt: null, accepted: false },
    pending: true,
    ...overrides,
  }
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ConsentGate>
        <p>conteúdo do app</p>
      </ConsentGate>
    </MemoryRouter>,
  )
}

describe('ConsentGate', () => {
  beforeEach(() => {
    state.status = { data: status(), isLoading: false }
    state.accept = { mutate: vi.fn(), isPending: false, isError: false, error: null }
    state.logout = vi.fn()
  })
  afterEach(cleanup)

  it('shows the app when nothing is pending', () => {
    state.status = { data: status({ pending: false }), isLoading: false }

    renderAt('/transacoes')

    expect(screen.getByText('conteúdo do app')).toBeTruthy()
  })

  it('blocks the app with the acceptance screen while a document is pending', () => {
    renderAt('/transacoes')

    expect(screen.queryByText('conteúdo do app')).toBeNull()
    expect(screen.getByText('Antes de continuar')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Li e aceito' })).toBeTruthy()
  })

  it('says the documents were updated when the person had accepted an older version', () => {
    state.status = {
      data: status({
        terms: { currentVersion: '2026-10-07', acceptedVersion: '2025-01-01', acceptedAt: '2025-01-01T10:00:00', accepted: false },
      }),
      isLoading: false,
    }

    renderAt('/')

    expect(screen.getByText('Atualizamos nossos documentos')).toBeTruthy()
  })

  it('sends back the versions shown on screen when the person accepts', () => {
    renderAt('/')

    fireEvent.click(screen.getByRole('button', { name: 'Li e aceito' }))

    expect(state.accept.mutate).toHaveBeenCalledWith({ termsVersion: '2026-10-07', privacyVersion: '2026-10-07' })
  })

  it('lets the person log out instead of accepting', () => {
    renderAt('/')

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(state.logout).toHaveBeenCalledTimes(1)
  })

  it('keeps the privacy settings reachable so data can be exported or the account deleted', () => {
    renderAt('/configuracoes/privacidade')

    expect(screen.getByText('conteúdo do app')).toBeTruthy()
  })

  it('does not lock the person out when the status request fails', () => {
    state.status = { data: undefined, isLoading: false }

    renderAt('/transacoes')

    expect(screen.getByText('conteúdo do app')).toBeTruthy()
  })

  it('waits for the status before showing anything', () => {
    state.status = { data: undefined, isLoading: true }

    renderAt('/')

    expect(screen.queryByText('conteúdo do app')).toBeNull()
    expect(screen.getByRole('status')).toBeTruthy()
  })
})
