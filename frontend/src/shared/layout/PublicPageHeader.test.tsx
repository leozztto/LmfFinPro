// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { PublicPageHeader } from './PublicPageHeader'

const state = vi.hoisted(() => ({ session: null as unknown }))

vi.mock('@/shared/auth/AuthContext', () => ({ useAuth: () => ({ session: state.session }) }))
vi.mock('@/shared/theme/ThemeToggle', () => ({ ThemeToggle: () => <button type="button">tema</button> }))

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/anterior', '/suporte']} initialIndex={1}>
      <Routes>
        <Route path="/" element={<p>dashboard</p>} />
        <Route path="/login" element={<p>login</p>} />
        <Route path="/anterior" element={<p>tela anterior</p>} />
        <Route path="/suporte" element={<PublicPageHeader />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PublicPageHeader', () => {
  beforeEach(() => {
    state.session = null
  })
  afterEach(cleanup)

  it('puts the back button first and the theme toggle last', () => {
    renderPage()

    const [first, last] = screen.getAllByRole('button')
    expect(first.getAttribute('aria-label')).toBe('Voltar')
    expect(first.getAttribute('title')).toBe('Voltar')
    expect(first.textContent).toBe('')
    expect(last.textContent).toBe('tema')
  })

  it('goes to the dashboard when logged in, whatever the history', () => {
    state.session = { userId: 1 }
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: 'Voltar' }))

    expect(screen.getByText('dashboard')).toBeTruthy()
  })

  it('goes to the login screen when logged out, whatever the history', () => {
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: 'Voltar' }))

    expect(screen.getByText('login')).toBeTruthy()
  })
})
