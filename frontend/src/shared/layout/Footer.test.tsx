// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { Footer } from './Footer'

describe('Footer', () => {
  afterEach(cleanup)

  it('shows the copyright and the contact icons, without the legal and support links', () => {
    render(<Footer />)

    expect(screen.getByText(/LEZZOTTO TECH LTDA/)).toBeTruthy()
    expect(screen.getByRole('link', { name: /Enviar e-mail/ }).getAttribute('href')).toBe('mailto:lezzottotech@gmail.com')
    expect(screen.getByRole('link', { name: 'Perfil no GitHub' }).getAttribute('rel')).toContain('noopener')
    expect(screen.getByRole('link', { name: 'Perfil no LinkedIn' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Portfólio' })).toBeTruthy()
    expect(screen.queryByText('Termos de Uso')).toBeNull()
    expect(screen.queryByText('Política de Privacidade')).toBeNull()
    expect(screen.queryByText('Suporte')).toBeNull()
  })
})
