import { clearActiveHousehold } from '@/shared/household/householdStorage'
import type { AuthSession } from './types'

export const SESSION_KEY = 'finpro.auth.session'

/** Disparado pelo httpClient quando o backend responde 401 (token ausente/expirado/inválido) —
 *  o AuthContext escuta esse evento pra sincronizar o estado React com a sessão já limpa.
 *  Um EventTarget próprio (em vez de `window`) funciona igual no browser e em testes Node. */
export const SESSION_EXPIRED_EVENT = 'finpro:session-expired'
export const authEvents = new EventTarget()

/** O que fica no localStorage: só dados de exibição. O access token vive apenas em memória (um XSS
 *  não consegue lê-lo do storage) e o refresh token fica em cookie httpOnly, fora do alcance do JS. */
export type StoredUser = Omit<AuthSession, 'token'>

let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

/** Lê o usuário salvo. Sessões antigas guardavam o token aqui: ele é descartado e o registro
 *  regravado sem ele, para que um JWT deixado pela versão anterior não fique exposto. */
export function loadStoredUser(): StoredUser | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<AuthSession>
    if (typeof parsed.userId !== 'number') return null
    const user: StoredUser = { userId: parsed.userId, name: parsed.name ?? '', email: parsed.email ?? '' }
    if ('token' in parsed) {
      localStorage.setItem(SESSION_KEY, JSON.stringify(user))
    }
    return user
  } catch {
    return null
  }
}

export function saveSession(session: AuthSession): void {
  accessToken = session.token
  const { userId, name, email } = session
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify({ userId, name, email }))
  } catch {
    // localStorage indisponível (modo privado, etc.) — o usuário só não é lembrado entre reloads.
  }
  sessionExpiredNotified = false
}

export function clearSession(): void {
  accessToken = null
  clearActiveHousehold()
  try {
    localStorage.removeItem(SESSION_KEY)
  } catch {
    // ver comentário em saveSession
  }
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
