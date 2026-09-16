import type { AuthSession } from './types'

const SESSION_KEY = 'finpro.auth.session'

export function loadSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as AuthSession) : null
  } catch {
    return null
  }
}

export function saveSession(session: AuthSession): void {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  } catch {
    // localStorage indisponível (modo privado, etc.) — a sessão simplesmente não persiste entre reloads.
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(SESSION_KEY)
  } catch {
    // ver comentário em saveSession
  }
}

export function getStoredToken(): string | null {
  return loadSession()?.token ?? null
}
