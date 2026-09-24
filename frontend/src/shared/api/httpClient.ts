import {
  SESSION_EXPIRED_EVENT,
  authEvents,
  clearSession,
  getStoredToken,
  markSessionExpiredOnce,
} from '@/shared/auth/authStorage'

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

async function request<TResponse>(path: string, options: RequestInit = {}): Promise<TResponse> {
  const token = getStoredToken()
  const headers = new Headers(options.headers)
  // FormData define seu próprio Content-Type (multipart/form-data; boundary=...) — o navegador
  // só consegue gerar o boundary correto se a gente não sobrescrever o header manualmente aqui.
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers })

  if (!response.ok) {
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
async function requestBlob(path: string): Promise<Blob> {
  const token = getStoredToken()
  const headers = new Headers()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { headers })

  if (!response.ok) {
    await handleErrorResponse(response)
  }

  return response.blob()
}

export const httpClient = {
  get: <TResponse>(path: string) => request<TResponse>(path, { method: 'GET' }),
  post: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
  postForm: <TResponse>(path: string, formData: FormData) => request<TResponse>(path, { method: 'POST', body: formData }),
  getBlob: (path: string) => requestBlob(path),
}
