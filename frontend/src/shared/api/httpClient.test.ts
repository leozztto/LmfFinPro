import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, httpClient } from './httpClient'

const { getStoredToken, clearSession, authEvents } = vi.hoisted(() => ({
  getStoredToken: vi.fn(),
  clearSession: vi.fn(),
  authEvents: new EventTarget(),
}))

vi.mock('@/shared/auth/authStorage', () => ({
  getStoredToken,
  clearSession,
  authEvents,
  SESSION_EXPIRED_EVENT: 'finpro:session-expired',
}))

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('httpClient', () => {
  beforeEach(() => {
    getStoredToken.mockReturnValue(null)
    clearSession.mockReset()
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
    getStoredToken.mockReturnValue('jwt-token')
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

  it('sends DELETE requests and returns undefined for a 204 response', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    const result = await httpClient.delete('/accounts/5')

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect(options?.method).toBe('DELETE')
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
})
