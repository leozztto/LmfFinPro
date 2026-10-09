// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { TourPreview } from './TourPreview'
import { TOUR_STEPS } from '../steps'

afterEach(cleanup)

describe('TourPreview', () => {
  it.each(TOUR_STEPS.map((step) => step.id))('draws an illustration for %s', (id) => {
    render(<TourPreview step={id} />)

    const preview = screen.getByTestId(`tour-preview-${id}`)
    expect(preview.textContent?.trim()).not.toBe('')
  })

  it('is decorative: hidden from assistive technology and not interactive', () => {
    render(<TourPreview step="ACCOUNT_FILL" />)

    const preview = screen.getByTestId('tour-preview-ACCOUNT_FILL')
    expect(preview.getAttribute('aria-hidden')).toBe('true')
    expect(preview.className).toContain('pointer-events-none')
    expect(preview.querySelector('input, button, a, select, textarea')).toBeNull()
  })

  it('highlights the button when saving and the fields when filling', () => {
    const { container, rerender } = render(<TourPreview step="TRANSACTION_FILL" />)
    const filling = container.querySelectorAll('.animate-pulse')

    rerender(<TourPreview step="TRANSACTION_SAVE" />)
    const saving = container.querySelectorAll('.animate-pulse')

    expect(filling.length).toBeGreaterThan(1)
    expect(saving).toHaveLength(1)
    expect(saving[0].textContent).toBe('Salvar')
  })

  it('shows the example values the person has to fill in', () => {
    render(<TourPreview step="RECURRING_FILL" />)

    expect(screen.getByText('Aluguel')).toBeTruthy()
    expect(screen.getByText('Mensal')).toBeTruthy()
  })

  it('shows the calendar with the day to look at highlighted', () => {
    const { container } = render(<TourPreview step="CALENDAR_VIEW" />)

    const highlighted = container.querySelectorAll('.animate-pulse')
    expect(highlighted).toHaveLength(1)
    expect(highlighted[0].textContent).toContain('10')
  })

  it('shows the dashboard figures highlighted', () => {
    render(<TourPreview step="DASHBOARD_VIEW" />)

    expect(screen.getByText('Saldo')).toBeTruthy()
    expect(screen.getByText('Entradas')).toBeTruthy()
    expect(screen.getByText('Saídas')).toBeTruthy()
  })

  it('shows the report options and the export button', () => {
    render(<TourPreview step="REPORTS_VIEW" />)

    expect(screen.getByText('Fluxo de caixa')).toBeTruthy()
    expect(screen.getByText('Exportar')).toBeTruthy()
  })
})
