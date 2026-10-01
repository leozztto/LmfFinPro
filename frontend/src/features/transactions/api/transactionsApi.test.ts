import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { transactionsApi } from './transactionsApi'

vi.mock('@/shared/auth/authStorage', () => ({
  getAccessToken: () => null,
  setAccessToken: vi.fn(),
  clearSession: vi.fn(),
  markSessionExpiredOnce: vi.fn(),
  authEvents: new EventTarget(),
  SESSION_EXPIRED_EVENT: 'finpro:session-expired',
}))

const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

function requestedUrl(): URL {
  return new URL(String(vi.mocked(fetch).mock.calls[0][0]))
}

describe('transactionsApi.list', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(EMPTY_PAGE), { status: 200, headers: { 'Content-Type': 'application/json' } }),
      ),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends only page and size when there are no filters', async () => {
    await transactionsApi.list({ page: 2, size: 20 })

    expect(requestedUrl().pathname).toBe('/api/transactions')
    expect(requestedUrl().search).toBe('?page=2&size=20')
  })

  it('sends every informed filter, repeating tagNames', async () => {
    await transactionsApi.list({
      page: 0,
      size: 20,
      accountId: 1,
      categoryId: 2,
      clientId: 3,
      type: 'INCOME',
      status: 'PENDING',
      hasAttachment: false,
      tagNames: ['projeto-acme', 'dedutivel'],
      startDate: '2026-01-01',
      endDate: '2026-01-31',
    })

    const params = requestedUrl().searchParams
    expect(params.get('accountId')).toBe('1')
    expect(params.get('categoryId')).toBe('2')
    expect(params.get('clientId')).toBe('3')
    expect(params.get('type')).toBe('INCOME')
    expect(params.get('status')).toBe('PENDING')
    expect(params.get('hasAttachment')).toBe('false')
    expect(params.getAll('tagNames')).toEqual(['projeto-acme', 'dedutivel'])
    expect(params.get('startDate')).toBe('2026-01-01')
    expect(params.get('endDate')).toBe('2026-01-31')
  })
})
