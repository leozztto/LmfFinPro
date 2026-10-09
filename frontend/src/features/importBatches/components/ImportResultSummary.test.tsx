// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ImportResultSummary } from './ImportResultSummary'
import type { ImportSummary } from '../types'

const { importBatchesApi } = vi.hoisted(() => ({
  importBatchesApi: { summary: vi.fn() },
}))

vi.mock('../api/importBatchesApi', () => ({ importBatchesApi }))

const SUMMARY: ImportSummary = {
  batchId: 7,
  transactionCount: 3,
  firstDate: '2026-01-05',
  lastDate: '2026-01-20',
  totalIncome: 4200,
  totalExpense: 150,
  balance: 4050,
  topExpenseCategories: [
    { categoryId: 1, name: 'Alimentação', total: 100, share: 66.7 },
    { categoryId: null, name: 'Sem categoria', total: 50, share: 33.3 },
  ],
  uncategorizedCount: 1,
}

function renderSummary(duplicateCount?: number) {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <ImportResultSummary batchId={7} duplicateCount={duplicateCount} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ImportResultSummary', () => {
  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('shows what came in, what went out and where most of it went', async () => {
    importBatchesApi.summary.mockResolvedValue(SUMMARY)

    renderSummary()

    expect(await screen.findByText('3 lançamentos importados')).toBeTruthy()
    expect(screen.getByText(/05\/01\/2026 a 20\/01\/2026/)).toBeTruthy()
    expect(screen.getByText('Entrou')).toBeTruthy()
    expect(screen.getByText('Saiu')).toBeTruthy()
    expect(screen.getByText('Saldo do período')).toBeTruthy()
    expect(screen.getByText('Alimentação')).toBeTruthy()
    expect(screen.getByText('Sem categoria')).toBeTruthy()
    expect(screen.getByText(/66,7%|66\.7%/)).toBeTruthy()
  })

  it('points to the review when some transactions have no category', async () => {
    importBatchesApi.summary.mockResolvedValue(SUMMARY)

    renderSummary(2)

    expect(await screen.findByRole('link', { name: 'Revisar agora' })).toBeTruthy()
    expect(screen.getByText(/2 já existia/)).toBeTruthy()
  })

  it('does not suggest a review when everything was categorized', async () => {
    importBatchesApi.summary.mockResolvedValue({
      ...SUMMARY,
      uncategorizedCount: 0,
    })

    renderSummary()

    await screen.findByText('3 lançamentos importados')
    expect(screen.queryByRole('link', { name: 'Revisar agora' })).toBeNull()
  })

  it('explains the failure without hiding that the import worked', async () => {
    importBatchesApi.summary.mockRejectedValue(new Error('falhou'))

    renderSummary()

    expect((await screen.findByRole('alert')).textContent).toContain('importação foi concluída')
  })
})
