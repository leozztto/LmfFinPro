// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useImportSummary } from './useImportSummary'

const { importBatchesApi } = vi.hoisted(() => ({ importBatchesApi: { summary: vi.fn() } }))

vi.mock('../api/importBatchesApi', () => ({ importBatchesApi }))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('useImportSummary', () => {
  it('does not query before there is a batch', () => {
    renderHook(() => useImportSummary(null), { wrapper })

    expect(importBatchesApi.summary).not.toHaveBeenCalled()
  })

  it('loads the summary of the batch', async () => {
    const summary = { transactionCount: 3, totalIncome: 4200, totalExpense: 50, balance: 4150 }
    importBatchesApi.summary.mockResolvedValue(summary)

    const { result } = renderHook(() => useImportSummary(9), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(summary))
    expect(importBatchesApi.summary.mock.calls[0]?.[0]).toBe(9)
  })
})
