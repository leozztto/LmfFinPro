// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Sidebar } from './Sidebar'

function renderSidebar(props: Parameters<typeof Sidebar>[0] = {}, path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Sidebar {...props} />
    </MemoryRouter>,
  )
}

afterEach(cleanup)

describe('Sidebar', () => {
  it('shows the icons with their texts and the section titles when open', () => {
    renderSidebar()

    expect(screen.getByText('Financeiro')).toBeTruthy()
    const link = screen.getByRole('link', { name: 'Transações' })
    expect(link.querySelector('svg')).not.toBeNull()
    expect(link.querySelector('.sr-only')).toBeNull()
    expect(link.getAttribute('title')).toBeNull()
  })

  it('keeps only the icons when collapsed, without losing the accessible name', () => {
    renderSidebar({ collapsed: true })

    const link = screen.getByRole('link', { name: 'Transações' })
    expect(link.querySelector('svg')).not.toBeNull()
    expect(link.querySelector('span')?.className).toContain('sr-only')
    expect(link.getAttribute('title')).toBe('Transações')
    expect(screen.queryByText('Financeiro')).toBeNull()
  })

  it('still lists every destination when collapsed', () => {
    renderSidebar({ collapsed: true })

    for (const name of ['Dashboard', 'Relatórios', 'Contas', 'Calendário', 'Clientes', 'Documentação', 'Suporte']) {
      expect(screen.getByRole('link', { name })).toBeTruthy()
    }
  })

  it('marks the current page and calls onNavigate when a link is used', () => {
    const onNavigate = vi.fn()
    renderSidebar({ collapsed: true, onNavigate }, '/metas')

    expect(screen.getByRole('link', { name: 'Metas' }).getAttribute('aria-current')).toBe('page')
    fireEvent.click(screen.getByRole('link', { name: 'Contas' }))
    expect(onNavigate).toHaveBeenCalledTimes(1)
  })
})
