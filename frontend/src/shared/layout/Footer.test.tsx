// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { Footer } from './Footer'

describe('Footer', () => {
  afterEach(cleanup)

  it('shows the copyright with only the e-mail and WhatsApp contacts', () => {
    render(<Footer />)

    expect(screen.getByText(/LEZZOTTO TECH LTDA/)).toBeTruthy()
    expect(screen.getByRole('link', { name: /Enviar e-mail/ }).getAttribute('href')).toBe('mailto:lezzottotech@gmail.com')
    const whatsapp = screen.getByRole('link', { name: 'Conversar pelo WhatsApp' })
    expect(whatsapp.getAttribute('href')).toContain('https://wa.me/5546991105807')
    expect(whatsapp.getAttribute('rel')).toContain('noopener')
    expect(screen.getAllByRole('link')).toHaveLength(2)
  })
})
