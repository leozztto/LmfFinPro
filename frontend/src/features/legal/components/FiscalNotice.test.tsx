// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { FiscalNotice } from './FiscalNotice'

describe('FiscalNotice', () => {
  afterEach(cleanup)

  it('says the estimates do not replace an accountant and links to the fiscal notice in the terms', () => {
    render(
      <MemoryRouter>
        <FiscalNotice subject="As estimativas de imposto" />
      </MemoryRouter>,
    )

    const note = screen.getByRole('note')
    expect(note.textContent).toContain('As estimativas de imposto são estimativas de referência')
    expect(note.textContent).toContain('Não substituem seu contador')
    expect(screen.getByRole('link', { name: 'Saiba mais' }).getAttribute('href')).toBe('/termos#aviso-fiscal')
  })
})
