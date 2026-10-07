import {
  SESSION_EXPIRED_EVENT,
  authEvents,
  clearSession,
  getAccessToken,
  markSessionExpiredOnce,
  setAccessToken,
} from '@/shared/auth/authStorage'
import {
  HOUSEHOLD_FORBIDDEN_EVENT,
  HOUSEHOLD_HEADER,
  getActiveHouseholdId,
  householdEvents,
} from '@/shared/household/householdStorage'

let apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

/** Só para os testes de contrato Pact: aponta as chamadas pro mock server que o Pact sobe em uma
 *  porta aleatória a cada teste — não existe forma de injetar isso via env var em tempo de teste
 *  porque `apiBaseUrl` já teria sido lido no import do módulo. */
export function __setApiBaseUrlForTests(url: string): void {
  apiBaseUrl = url
}

interface ApiErrorBody {
  message?: string
}

/** Por padrão a requisição usa o grupo que a pessoa está vendo. `householdId` força um grupo
 *  específico (ex.: listar as contas do espaço pessoal enquanto se vê um grupo compartilhado). */
export interface RequestScope {
  householdId?: number
}

/** Gestão de grupos e autenticação não dependem do grupo ativo: com um grupo ativo já inválido (a
 *  pessoa foi removida) o header faria até a lista de grupos dar 403, impedindo a recuperação. */
const HOUSEHOLD_AGNOSTIC_PREFIXES = ['/auth/', '/households']

function householdFor(path: string, scope?: RequestScope): number | null {
  if (scope?.householdId !== undefined) return scope.householdId
  if (HOUSEHOLD_AGNOSTIC_PREFIXES.some((prefix) => path.startsWith(prefix))) return null
  return getActiveHouseholdId()
}

/** 403 com um grupo escolhido: avisa a UI para conferir se a pessoa ainda participa dele. */
function notifyIfHouseholdForbidden(response: Response, householdId: number | null): void {
  if (response.status === 403 && householdId !== null) {
    householdEvents.dispatchEvent(new Event(HOUSEHOLD_FORBIDDEN_EVENT))
  }
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

/** Token ausente/expirado/inválido: sem isso a sessão continua "logada" no localStorage pra
 *  sempre, e toda tela fica com spinners que nunca resolvem — nada nunca limpava a sessão nem
 *  avisava a UI que ela morreu. */
async function handleErrorResponse(response: Response): Promise<never> {
  const body = (await response.json().catch(() => null)) as ApiErrorBody | null
  if (response.status === 401) {
    clearSession()
    // Requisições em paralelo (ex: Dashboard) recebem 401 quase ao mesmo tempo quando a sessão
    // expira; sem essa trava o evento dispararia uma vez por requisição, empilhando toasts.
    if (markSessionExpiredOnce()) {
      authEvents.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    }
  }
  throw new ApiError(response.status, body?.message ?? 'Erro ao comunicar com o servidor')
}

export interface RefreshedSession {
  token: string
  userId: number
  name: string
  email: string
}

let refreshInFlight: Promise<RefreshedSession | null> | null = null

/** Troca o refresh token (cookie httpOnly, que o JS nem enxerga) por um access token novo e o
 *  guarda em memória. Null = não há sessão renovável (cookie ausente, expirado ou revogado).
 *  Chamadas simultâneas compartilham a mesma requisição: o refresh token é rotacionado a cada uso,
 *  então duas chamadas em paralelo com o mesmo cookie seriam tratadas como reuso. */
export function refreshSession(): Promise<RefreshedSession | null> {
  refreshInFlight ??= requestRefresh().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

async function requestRefresh(): Promise<RefreshedSession | null> {
  try {
    const response = await fetch(`${apiBaseUrl}/auth/refresh`, { method: 'POST', credentials: 'include' })
    if (!response.ok) return null
    const session = (await response.json()) as RefreshedSession
    setAccessToken(session.token)
    return session
  } catch {
    return null
  }
}

/** Avisa o backend para revogar o refresh token e apagar o cookie. Falha de rede é ignorada: a
 *  sessão local já foi encerrada, e o token sem uso expira sozinho. */
export async function endRemoteSession(): Promise<void> {
  try {
    await fetch(`${apiBaseUrl}/auth/logout`, { method: 'POST', credentials: 'include' })
  } catch {
    // ver comentário acima
  }
}

async function request<TResponse>(
  path: string,
  options: RequestInit = {},
  canRetryAfterRefresh = true,
  scope?: RequestScope,
): Promise<TResponse> {
  const token = getAccessToken()
  const headers = new Headers(options.headers)
  // FormData define seu próprio Content-Type (multipart/form-data; boundary=...) — o navegador
  // só consegue gerar o boundary correto se a gente não sobrescrever o header manualmente aqui.
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const householdId = householdFor(path, scope)
  if (householdId !== null) {
    headers.set(HOUSEHOLD_HEADER, String(householdId))
  }

  // `include`: em dev a API está em outra origem (localhost:8080) e só assim o navegador aceita o
  // Set-Cookie do refresh token devolvido pelo login/cadastro/troca de senha.
  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers, credentials: 'include' })

  // Access token expirado (vida curta): renova pelo cookie e repete a chamada uma única vez. Se
  // outra requisição já renovou enquanto esta voava, o token em memória mudou e basta repetir.
  if (response.status === 401 && token && canRetryAfterRefresh && !path.startsWith('/auth/')) {
    if (getAccessToken() !== token || (await refreshSession())) {
      return request<TResponse>(path, options, false, scope)
    }
  }

  if (!response.ok) {
    notifyIfHouseholdForbidden(response, householdId)
    await handleErrorResponse(response)
  }

  // 204 e respostas sem corpo (ex: 202 do "esqueci minha senha") não têm JSON para ler.
  const text = await response.text()
  if (!text) {
    return undefined as TResponse
  }

  return JSON.parse(text) as TResponse
}

/** Para downloads de arquivo (ex: PDF de relatório) — resposta não é JSON. */
async function requestBlob(path: string, canRetryAfterRefresh = true): Promise<Blob> {
  const token = getAccessToken()
  const headers = new Headers()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const householdId = householdFor(path)
  if (householdId !== null) {
    headers.set(HOUSEHOLD_HEADER, String(householdId))
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { headers })

  if (response.status === 401 && token && canRetryAfterRefresh) {
    if (getAccessToken() !== token || (await refreshSession())) {
      return requestBlob(path, false)
    }
  }

  if (!response.ok) {
    notifyIfHouseholdForbidden(response, householdId)
    await handleErrorResponse(response)
  }

  return response.blob()
}

export const httpClient = {
  get: <TResponse>(path: string, scope?: RequestScope) =>
    request<TResponse>(path, { method: 'GET' }, true, scope),
  post: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string, body?: unknown) =>
    request<void>(path, { method: 'DELETE', body: body === undefined ? undefined : JSON.stringify(body) }),
  postForm: <TResponse>(path: string, formData: FormData) => request<TResponse>(path, { method: 'POST', body: formData }),
  putForm: <TResponse>(path: string, formData: FormData) => request<TResponse>(path, { method: 'PUT', body: formData }),
  getBlob: (path: string) => requestBlob(path),
}
