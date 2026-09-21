import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearSession, getStoredToken, loadSession, saveSession } from './authStorage'
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
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loadSession returns null when nothing was saved', () => {
    expect(loadSession()).toBeNull()
  })

  it('saveSession then loadSession round-trips the same session', () => {
    saveSession(sampleSession)

    expect(loadSession()).toEqual(sampleSession)
  })

  it('loadSession returns null when the stored value is not valid JSON', () => {
    localStorage.setItem('finpro.auth.session', '{not-json')

    expect(loadSession()).toBeNull()
  })

  it('clearSession removes the saved session', () => {
    saveSession(sampleSession)

    clearSession()

    expect(loadSession()).toBeNull()
  })

  it('getStoredToken returns the token from the saved session', () => {
    saveSession(sampleSession)

    expect(getStoredToken()).toBe('jwt-token')
  })

  it('getStoredToken returns null when there is no saved session', () => {
    expect(getStoredToken()).toBeNull()
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
})
