import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, endRemoteSession, httpClient, refreshSession } from './httpClient'
import {
  HOUSEHOLD_FORBIDDEN_EVENT,
  HOUSEHOLD_HEADER,
  clearActiveHousehold,
  householdEvents,
  setActiveHouseholdId,
} from '@/shared/household/householdStorage'

const { getAccessToken, setAccessToken, clearSession, markSessionExpiredOnce, authEvents } = vi.hoisted(() => ({
  getAccessToken: vi.fn(),
  setAccessToken: vi.fn(),
  clearSession: vi.fn(),
  markSessionExpiredOnce: vi.fn(),
  authEvents: new EventTarget(),
}))

vi.mock('@/shared/auth/authStorage', () => ({
  getAccessToken,
  setAccessToken,
  clearSession,
  markSessionExpiredOnce,
  authEvents,
  SESSION_EXPIRED_EVENT: 'finpro:session-expired',
}))

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('httpClient', () => {
  beforeEach(() => {
    getAccessToken.mockReturnValue(null)
    clearSession.mockReset()
    markSessionExpiredOnce.mockReset()
    markSessionExpiredOnce.mockReturnValue(true)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('sends GET requests to the configured API base URL without an Authorization header when not logged in', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ id: 1 }))

    await httpClient.get('/accounts')

    const [url, options] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/accounts')
    expect(options?.method).toBe('GET')
    expect((options?.headers as Headers).get('Authorization')).toBeNull()
    expect((options?.headers as Headers).get('Content-Type')).toBe('application/json')
  })

  it('adds a Bearer Authorization header when a token is stored', async () => {
    getAccessToken.mockReturnValue('jwt-token')
    vi.mocked(fetch).mockResolvedValue(jsonResponse({}))

    await httpClient.get('/accounts')

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect((options?.headers as Headers).get('Authorization')).toBe('Bearer jwt-token')
  })

  it('sends POST requests with a JSON-serialized body', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ id: 5 }, 201))

    const result = await httpClient.post('/accounts', { name: 'Carteira' })

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ name: 'Carteira' }))
    expect(result).toEqual({ id: 5 })
  })

  it('sends PUT requests with a JSON-serialized body', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ id: 5, name: 'Atualizado' }))

    await httpClient.put('/accounts/5', { name: 'Atualizado' })

    const [url, options] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/accounts/5')
    expect(options?.method).toBe('PUT')
  })

  it('sends PATCH requests with a JSON-serialized body', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ id: 7, status: 'PAID' }))

    await httpClient.patch('/transactions/7/status', { status: 'PAID' })

    const [url, options] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/transactions/7/status')
    expect(options?.method).toBe('PATCH')
    expect(options?.body).toBe(JSON.stringify({ status: 'PAID' }))
  })

  it('sends DELETE requests and returns undefined for a 204 response', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    const result = await httpClient.delete('/accounts/5')

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect(options?.method).toBe('DELETE')
    expect(result).toBeUndefined()
  })

  it('returns undefined for a 202 response without body', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 202 }))

    const result = await httpClient.post('/auth/forgot-password', { email: 'ana@finpro.test' })

    expect(result).toBeUndefined()
  })

  it('sends FormData bodies without forcing a JSON Content-Type header', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({}))
    const formData = new FormData()
    formData.append('file', new Blob(['a,b,c']), 'extrato.csv')

    await httpClient.postForm('/import-batches', formData)

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect(options?.body).toBe(formData)
    expect((options?.headers as Headers).has('Content-Type')).toBe(false)
  })

  it('throws an ApiError with the server message when the response is not ok', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'CPF inválido' }, 400))

    await expect(httpClient.get('/accounts')).rejects.toMatchObject(
      new ApiError(400, 'CPF inválido'),
    )
  })

  it('falls back to a default message when the error response has no JSON body', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('not json', { status: 500 }))

    await expect(httpClient.get('/accounts')).rejects.toMatchObject(
      new ApiError(500, 'Erro ao comunicar com o servidor'),
    )
  })

  it('does not clear the session or emit the expired event for a non-401 error', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'CPF inválido' }, 400))

    await expect(httpClient.get('/accounts')).rejects.toThrow()

    expect(clearSession).not.toHaveBeenCalled()
  })

  it('clears the session and emits the session-expired event on a 401 response', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Token inválido' }, 401))
    const listener = vi.fn()
    authEvents.addEventListener('finpro:session-expired', listener)

    await expect(httpClient.get('/accounts')).rejects.toMatchObject(new ApiError(401, 'Token inválido'))

    expect(clearSession).toHaveBeenCalledTimes(1)
    expect(listener).toHaveBeenCalledTimes(1)
    authEvents.removeEventListener('finpro:session-expired', listener)
  })

  it('always sends the cookies (credentials: include) so the refresh cookie is stored and sent', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({}))

    await httpClient.get('/accounts')

    expect(vi.mocked(fetch).mock.calls[0][1]?.credentials).toBe('include')
  })

  describe('access token refresh', () => {
    const refreshed = { token: 'new-jwt', userId: 1, name: 'Ana', email: 'ana@finpro.test' }

    it('refreshes on a 401 and retries the request once with the new token', async () => {
      // Simula o armazenamento real: o refresh troca o token em memória.
      let current: string | null = 'old-jwt'
      getAccessToken.mockImplementation(() => current)
      setAccessToken.mockImplementation((t: string | null) => {
        current = t
      })
      vi.mocked(fetch)
        .mockResolvedValueOnce(jsonResponse({ message: 'expirado' }, 401))
        .mockResolvedValueOnce(jsonResponse(refreshed))
        .mockResolvedValueOnce(jsonResponse({ id: 1 }))

      const result = await httpClient.get('/accounts')

      expect(result).toEqual({ id: 1 })
      const calls = vi.mocked(fetch).mock.calls
      expect(calls[1][0]).toBe('http://localhost:8080/api/auth/refresh')
      expect(calls[1][1]?.method).toBe('POST')
      expect((calls[2][1]?.headers as Headers).get('Authorization')).toBe('Bearer new-jwt')
      expect(clearSession).not.toHaveBeenCalled()
    })

    it('clears the session and emits the expired event when the refresh fails', async () => {
      getAccessToken.mockReturnValue('old-jwt')
      vi.mocked(fetch)
        .mockResolvedValueOnce(jsonResponse({ message: 'expirado' }, 401))
        .mockResolvedValueOnce(new Response(null, { status: 401 }))
      const listener = vi.fn()
      authEvents.addEventListener('finpro:session-expired', listener)

      await expect(httpClient.get('/accounts')).rejects.toMatchObject(new ApiError(401, 'expirado'))

      expect(fetch).toHaveBeenCalledTimes(2)
      expect(clearSession).toHaveBeenCalledTimes(1)
      expect(listener).toHaveBeenCalledTimes(1)
      authEvents.removeEventListener('finpro:session-expired', listener)
    })

    it('does not loop: a second 401 after a successful refresh ends the session', async () => {
      let current: string | null = 'old-jwt'
      getAccessToken.mockImplementation(() => current)
      setAccessToken.mockImplementation((t: string | null) => {
        current = t
      })
      vi.mocked(fetch)
        .mockResolvedValueOnce(jsonResponse({ message: 'x' }, 401))
        .mockResolvedValueOnce(jsonResponse(refreshed))
        .mockResolvedValueOnce(jsonResponse({ message: 'ainda 401' }, 401))

      await expect(httpClient.get('/accounts')).rejects.toMatchObject(new ApiError(401, 'ainda 401'))

      expect(fetch).toHaveBeenCalledTimes(3)
      expect(clearSession).toHaveBeenCalledTimes(1)
    })

    it('does not try to refresh when the request carried no token (e.g. wrong password on login)', async () => {
      getAccessToken.mockReturnValue(null)
      vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'E-mail ou senha inválidos' }, 401))

      await expect(httpClient.post('/auth/login', {})).rejects.toThrow()

      expect(fetch).toHaveBeenCalledTimes(1)
    })

    it('does not try to refresh for /auth/* endpoints even with a token in memory', async () => {
      getAccessToken.mockReturnValue('old-jwt')
      vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'x' }, 401))

      await expect(httpClient.post('/auth/reset-password', {})).rejects.toThrow()

      expect(fetch).toHaveBeenCalledTimes(1)
    })

    it('shares a single refresh call between parallel 401s (the refresh token is rotated on use)', async () => {
      let current: string | null = 'old-jwt'
      getAccessToken.mockImplementation(() => current)
      setAccessToken.mockImplementation((t: string | null) => {
        current = t
      })
      vi.mocked(fetch).mockImplementation(async (input) => {
        const url = String(input)
        if (url.endsWith('/auth/refresh')) return jsonResponse(refreshed)
        return current === 'new-jwt' ? jsonResponse({ ok: true }) : jsonResponse({ message: 'x' }, 401)
      })

      await Promise.all([httpClient.get('/accounts'), httpClient.get('/categories')])

      const refreshCalls = vi.mocked(fetch).mock.calls.filter(([url]) => String(url).endsWith('/auth/refresh'))
      expect(refreshCalls).toHaveLength(1)
    })

    it('refreshSession returns null on network failure instead of throwing', async () => {
      vi.mocked(fetch).mockRejectedValue(new TypeError('Failed to fetch'))

      await expect(refreshSession()).resolves.toBeNull()
    })

    it('endRemoteSession calls /auth/logout with credentials and swallows failures', async () => {
      vi.mocked(fetch).mockRejectedValue(new TypeError('Failed to fetch'))

      await expect(endRemoteSession()).resolves.toBeUndefined()

      const [url, options] = vi.mocked(fetch).mock.calls[0]
      expect(url).toBe('http://localhost:8080/api/auth/logout')
      expect(options?.credentials).toBe('include')
    })
  })

  describe('getBlob', () => {
    const refreshed = { token: 'new-jwt', userId: 1, name: 'Ana', email: 'ana@finpro.test' }

    function useRealTokenStore(initial: string | null) {
      let current = initial
      getAccessToken.mockImplementation(() => current)
      setAccessToken.mockImplementation((t: string | null) => {
        current = t
      })
      return (t: string | null) => {
        current = t
      }
    }

    it('returns the response body as a Blob, sending the Authorization header', async () => {
      getAccessToken.mockReturnValue('jwt')
      vi.mocked(fetch).mockResolvedValue(new Response('pdf-bytes', { status: 200 }))

      const blob = await httpClient.getBlob('/reports/1/pdf')

      expect(await blob.text()).toBe('pdf-bytes')
      const [url, options] = vi.mocked(fetch).mock.calls[0]
      expect(url).toBe('http://localhost:8080/api/reports/1/pdf')
      expect((options?.headers as Headers).get('Authorization')).toBe('Bearer jwt')
    })

    it('refreshes on a 401 and retries the download once with the new token', async () => {
      useRealTokenStore('old-jwt')
      vi.mocked(fetch)
        .mockResolvedValueOnce(new Response(null, { status: 401 }))
        .mockResolvedValueOnce(jsonResponse(refreshed))
        .mockResolvedValueOnce(new Response('pdf-bytes', { status: 200 }))

      const blob = await httpClient.getBlob('/reports/1/pdf')

      expect(await blob.text()).toBe('pdf-bytes')
      const calls = vi.mocked(fetch).mock.calls
      expect(calls[1][0]).toBe('http://localhost:8080/api/auth/refresh')
      expect((calls[2][1]?.headers as Headers).get('Authorization')).toBe('Bearer new-jwt')
      expect(clearSession).not.toHaveBeenCalled()
    })

    it('retries without calling refresh when another request already renewed the token', async () => {
      const setToken = useRealTokenStore('old-jwt')
      vi.mocked(fetch)
        .mockImplementationOnce(async () => {
          setToken('renewed-jwt')
          return new Response(null, { status: 401 })
        })
        .mockResolvedValueOnce(new Response('pdf-bytes', { status: 200 }))

      await httpClient.getBlob('/reports/1/pdf')

      expect(fetch).toHaveBeenCalledTimes(2)
      const calls = vi.mocked(fetch).mock.calls
      expect(calls.some(([url]) => String(url).endsWith('/auth/refresh'))).toBe(false)
      expect((calls[1][1]?.headers as Headers).get('Authorization')).toBe('Bearer renewed-jwt')
    })

    it('ends the session when the refresh fails', async () => {
      getAccessToken.mockReturnValue('old-jwt')
      vi.mocked(fetch)
        .mockResolvedValueOnce(jsonResponse({ message: 'expirado' }, 401))
        .mockResolvedValueOnce(new Response(null, { status: 401 }))

      await expect(httpClient.getBlob('/reports/1/pdf')).rejects.toMatchObject(new ApiError(401, 'expirado'))

      expect(fetch).toHaveBeenCalledTimes(2)
      expect(clearSession).toHaveBeenCalledTimes(1)
    })

    it('does not loop: a second 401 after a successful refresh ends the session', async () => {
      useRealTokenStore('old-jwt')
      vi.mocked(fetch)
        .mockResolvedValueOnce(new Response(null, { status: 401 }))
        .mockResolvedValueOnce(jsonResponse(refreshed))
        .mockResolvedValueOnce(jsonResponse({ message: 'ainda 401' }, 401))

      await expect(httpClient.getBlob('/reports/1/pdf')).rejects.toMatchObject(new ApiError(401, 'ainda 401'))

      expect(fetch).toHaveBeenCalledTimes(3)
      expect(clearSession).toHaveBeenCalledTimes(1)
    })

    it('does not try to refresh when the request carried no token', async () => {
      getAccessToken.mockReturnValue(null)
      vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Não autenticado' }, 401))

      await expect(httpClient.getBlob('/reports/1/pdf')).rejects.toThrow()

      expect(fetch).toHaveBeenCalledTimes(1)
    })

    it('throws an ApiError for non-401 failures', async () => {
      getAccessToken.mockReturnValue('jwt')
      vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Relatório não encontrado' }, 404))

      await expect(httpClient.getBlob('/reports/9/pdf')).rejects.toMatchObject(
        new ApiError(404, 'Relatório não encontrado'),
      )
      expect(clearSession).not.toHaveBeenCalled()
    })
  })

  it('does not emit the session-expired event again when markSessionExpiredOnce says it already fired', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Token inválido' }, 401))
    markSessionExpiredOnce.mockReturnValue(false)
    const listener = vi.fn()
    authEvents.addEventListener('finpro:session-expired', listener)

    // Simula duas requisições em paralelo recebendo 401 ao mesmo tempo (ex: Dashboard).
    await expect(httpClient.get('/accounts')).rejects.toThrow()
    await expect(httpClient.get('/categories')).rejects.toThrow()

    expect(clearSession).toHaveBeenCalledTimes(2)
    expect(listener).not.toHaveBeenCalled()
    authEvents.removeEventListener('finpro:session-expired', listener)
  })
})

describe('httpClient — grupo (household) ativo', () => {
  beforeEach(() => {
    getAccessToken.mockReturnValue('jwt-token')
    vi.stubGlobal('fetch', vi.fn())
    clearActiveHousehold()
  })

  afterEach(() => {
    clearActiveHousehold()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  function sentHeaders(): Headers {
    return vi.mocked(fetch).mock.calls[0][1]?.headers as Headers
  }

  it('sends no household header in the personal space (nothing chosen)', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([]))

    await httpClient.get('/accounts')

    expect(sentHeaders().get(HOUSEHOLD_HEADER)).toBeNull()
  })

  it('sends the chosen group in the X-Household-Id header', async () => {
    setActiveHouseholdId(7)
    vi.mocked(fetch).mockImplementation(() => Promise.resolve(jsonResponse([])))

    await httpClient.get('/accounts')
    await httpClient.post('/transactions', { description: 'x' })

    const calls = vi.mocked(fetch).mock.calls
    expect((calls[0][1]?.headers as Headers).get(HOUSEHOLD_HEADER)).toBe('7')
    expect((calls[1][1]?.headers as Headers).get(HOUSEHOLD_HEADER)).toBe('7')
  })

  it('does not send the header to /households and /auth, which must work even with a stale group', async () => {
    setActiveHouseholdId(7)
    vi.mocked(fetch).mockImplementation(() => Promise.resolve(jsonResponse([])))

    await httpClient.get('/households')
    await httpClient.post('/households/10/accounts/share', { accountIds: [1] })
    await httpClient.post('/auth/login', {})

    for (const [, options] of vi.mocked(fetch).mock.calls) {
      expect((options?.headers as Headers).get(HOUSEHOLD_HEADER)).toBeNull()
    }
  })

  it('an explicit scope overrides the active group, even on /households-agnostic reads', async () => {
    setActiveHouseholdId(7)
    vi.mocked(fetch).mockResolvedValue(jsonResponse([]))

    await httpClient.get('/accounts', { householdId: 3 })

    expect(sentHeaders().get(HOUSEHOLD_HEADER)).toBe('3')
  })

  it('keeps the explicit scope when the request is retried after a token refresh', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse({}, 401))
      .mockResolvedValueOnce(jsonResponse({ token: 'novo', userId: 1, name: 'A', email: 'a@a.com' }))
      .mockResolvedValueOnce(jsonResponse([]))

    await httpClient.get('/accounts', { householdId: 3 })

    const retry = vi.mocked(fetch).mock.calls[2][1]?.headers as Headers
    expect(retry.get(HOUSEHOLD_HEADER)).toBe('3')
  })

  it('also sends the group on file downloads', async () => {
    setActiveHouseholdId(7)
    vi.mocked(fetch).mockResolvedValue(new Response(new Blob(['pdf']), { status: 200 }))

    await httpClient.getBlob('/reports/transactions')

    expect(sentHeaders().get(HOUSEHOLD_HEADER)).toBe('7')
  })

  it('announces a 403 received with a chosen group, so the UI can check the membership', async () => {
    setActiveHouseholdId(7)
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Você não participa deste grupo' }, 403))
    const listener = vi.fn()
    householdEvents.addEventListener(HOUSEHOLD_FORBIDDEN_EVENT, listener)

    await expect(httpClient.get('/accounts')).rejects.toBeInstanceOf(ApiError)

    householdEvents.removeEventListener(HOUSEHOLD_FORBIDDEN_EVENT, listener)
    expect(listener).toHaveBeenCalledTimes(1)
  })

  it('does not announce a 403 when no group was chosen', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: 'Proibido' }, 403))
    const listener = vi.fn()
    householdEvents.addEventListener(HOUSEHOLD_FORBIDDEN_EVENT, listener)

    await expect(httpClient.get('/accounts')).rejects.toBeInstanceOf(ApiError)

    householdEvents.removeEventListener(HOUSEHOLD_FORBIDDEN_EVENT, listener)
    expect(listener).not.toHaveBeenCalled()
  })
})
