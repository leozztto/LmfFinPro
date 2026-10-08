// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthPageShell } from './AuthPageShell'

vi.mock('@/shared/theme/ThemeToggle', () => ({ ThemeToggle: () => null }))

describe('AuthPageShell', () => {
  afterEach(cleanup)

  it('offers a support link on the public access screens', () => {
    render(
      <MemoryRouter>
        <AuthPageShell>
          <p>formulário</p>
        </AuthPageShell>
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Suporte e contato' }).getAttribute('href')).toBe('/suporte')
    expect(screen.getByText('formulário')).toBeTruthy()
  })
})
