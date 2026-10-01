import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  SESSION_KEY,
  clearSession,
  getAccessToken,
  loadStoredUser,
  markSessionExpiredOnce,
  saveSession,
  setAccessToken,
} from './authStorage'
import type { AuthSession } from './types'

function createLocalStorageMock() {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => {
      store[key] = value
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      store = {}
    },
  }
}

const sampleSession: AuthSession = {
  token: 'jwt-token',
  userId: 1,
  name: 'Ana',
  email: 'ana@finpro.test',
}

describe('authStorage', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', createLocalStorageMock())
    setAccessToken(null)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loadStoredUser returns null when nothing was saved', () => {
    expect(loadStoredUser()).toBeNull()
  })

  it('saveSession then loadStoredUser round-trips the user without the token', () => {
    saveSession(sampleSession)

    expect(loadStoredUser()).toEqual({ userId: 1, name: 'Ana', email: 'ana@finpro.test' })
  })

  it('never writes the access token to localStorage (XSS could read it)', () => {
    saveSession(sampleSession)

    expect(localStorage.getItem(SESSION_KEY)).not.toContain('jwt-token')
    expect(localStorage.getItem(SESSION_KEY)).not.toContain('token')
  })

  it('keeps the access token in memory after saveSession', () => {
    saveSession(sampleSession)

    expect(getAccessToken()).toBe('jwt-token')
  })

  it('loadStoredUser returns null when the stored value is not valid JSON', () => {
    localStorage.setItem(SESSION_KEY, '{not-json')

    expect(loadStoredUser()).toBeNull()
  })

  it('loadStoredUser strips a legacy token left in localStorage by the previous version', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(sampleSession))

    expect(loadStoredUser()).toEqual({ userId: 1, name: 'Ana', email: 'ana@finpro.test' })
    expect(localStorage.getItem(SESSION_KEY)).not.toContain('jwt-token')
    // O token legado não vira token em memória: a sessão é retomada pelo refresh (cookie).
    expect(getAccessToken()).toBeNull()
  })

  it('clearSession removes the user and the in-memory token', () => {
    saveSession(sampleSession)

    clearSession()

    expect(loadStoredUser()).toBeNull()
    expect(getAccessToken()).toBeNull()
  })

  it('getAccessToken returns null when there is no session', () => {
    expect(getAccessToken()).toBeNull()
  })

  it('saveSession swallows errors when localStorage is unavailable (e.g. modo privado)', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => {
        throw new Error('QuotaExceededError')
      },
      removeItem: () => {},
    })

    expect(() => saveSession(sampleSession)).not.toThrow()
    expect(getAccessToken()).toBe('jwt-token')
  })

  it('clearSession swallows errors when localStorage is unavailable', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => {},
      removeItem: () => {
        throw new Error('unavailable')
      },
    })

    expect(() => clearSession()).not.toThrow()
  })

  describe('markSessionExpiredOnce', () => {
    beforeEach(() => {
      // saveSession reseta a trava — garante que cada teste começa como se tivesse acabado de logar.
      saveSession(sampleSession)
    })

    it('returns true the first time it is called after a login', () => {
      expect(markSessionExpiredOnce()).toBe(true)
    })

    it('returns false for subsequent calls, so parallel 401s only notify once', () => {
      expect(markSessionExpiredOnce()).toBe(true)
      expect(markSessionExpiredOnce()).toBe(false)
      expect(markSessionExpiredOnce()).toBe(false)
    })

    it('returns true again after a new session is saved (re-login)', () => {
      expect(markSessionExpiredOnce()).toBe(true)

      saveSession(sampleSession)

      expect(markSessionExpiredOnce()).toBe(true)
    })
  })
})
