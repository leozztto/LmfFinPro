import type { AuthSession } from './types'

const SESSION_KEY = 'finpro.auth.session'

/** Disparado pelo httpClient quando o backend responde 401 (token ausente/expirado/inválido) —
 *  o AuthContext escuta esse evento pra sincronizar o estado React com a sessão já limpa.
 *  Um EventTarget próprio (em vez de `window`) funciona igual no browser e em testes Node. */
export const SESSION_EXPIRED_EVENT = 'finpro:session-expired'
export const authEvents = new EventTarget()

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
  sessionExpiredNotified = false
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

let sessionExpiredNotified = false

/** Uma tela costuma disparar várias requisições em paralelo (ex: Dashboard); quando a sessão
 *  expira, todas elas recebem 401 quase ao mesmo tempo. Sem essa trava, o httpClient dispararia
 *  SESSION_EXPIRED_EVENT uma vez por requisição e o usuário veria vários toasts empilhados.
 *  Retorna true apenas na primeira chamada após um login — as seguintes retornam false até a
 *  próxima sessão ser criada (saveSession reseta a trava). */
export function markSessionExpiredOnce(): boolean {
  if (sessionExpiredNotified) return false
  sessionExpiredNotified = true
  return true
}
