// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SupportPage } from './SupportPage'

const state = vi.hoisted(() => ({
  status: { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: vi.fn() } as {
    data?: unknown
    isLoading: boolean
    isError: boolean
    isFetching: boolean
    refetch: () => void
  },
}))

vi.mock('../hooks/usePlatformStatus', () => ({ usePlatformStatus: () => state.status }))
vi.mock('@/shared/theme/ThemeToggle', () => ({ ThemeToggle: () => null }))
vi.mock('@/shared/layout/Footer', () => ({ Footer: () => <footer>rodapé</footer> }))

function renderPage() {
  return render(
    <MemoryRouter>
      <SupportPage />
    </MemoryRouter>,
  )
}

describe('SupportPage', () => {
  beforeEach(() => {
    state.status = { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: vi.fn() }
  })
  afterEach(cleanup)

  it('shows the e-mail channel and a phone that opens WhatsApp', () => {
    renderPage()

    expect(screen.getByRole('link', { name: /lezzottotech@gmail.com/ }).getAttribute('href')).toBe(
      'mailto:lezzottotech@gmail.com',
    )
    const phone = screen.getByRole('link', { name: '(46) 99110-5807' })
    expect(phone.getAttribute('target')).toBe('_blank')
    expect(phone.getAttribute('rel')).toContain('noopener')
    expect(phone.getAttribute('href')).toBe('https://wa.me/5546991105807?text=Ol%C3%A1!%20Preciso%20de%20ajuda%20com%20o%20FinPro.')
  })

  it('shows the platform operational', () => {
    state.status = { ...state.status, data: { status: 'OPERATIONAL', checkedAt: '2026-10-08T12:00:00' } }

    renderPage()

    expect(screen.getByText('Todos os sistemas estão operacionais.')).toBeTruthy()
    expect(screen.queryByText('Banco de dados')).toBeNull()
  })

  it('warns about an instability without detailing which part', () => {
    state.status = { ...state.status, data: { status: 'OUTAGE', checkedAt: '2026-10-08T12:00:00' } }

    renderPage()

    expect(screen.getByText(/Estamos com uma instabilidade/)).toBeTruthy()
    expect(screen.queryByText('Banco de dados')).toBeNull()
  })

  it('tells the person when the status itself cannot be read', () => {
    state.status = { ...state.status, isError: true }

    renderPage()

    expect(screen.getByRole('alert').textContent).toContain('Não conseguimos falar com o servidor')
  })

  it('shows the app footer', () => {
    renderPage()

    expect(screen.getByText('rodapé')).toBeTruthy()
  })

  it('refreshes on demand', () => {
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }))

    expect(state.status.refetch).toHaveBeenCalledTimes(1)
  })
})
