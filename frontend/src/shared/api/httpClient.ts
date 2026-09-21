import { SESSION_EXPIRED_EVENT, authEvents, clearSession, getStoredToken } from '@/shared/auth/authStorage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

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

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null
    // Token ausente/expirado/inválido: sem isso a sessão continua "logada" no localStorage pra
    // sempre, e toda tela fica com spinners que nunca resolvem — nada nunca limpava a sessão nem
    // avisava a UI que ela morreu.
    if (response.status === 401) {
      clearSession()
      authEvents.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    }
    throw new ApiError(response.status, body?.message ?? 'Erro ao comunicar com o servidor')
  }

  if (response.status === 204) {
    return undefined as TResponse
  }

  return (await response.json()) as TResponse
}

export const httpClient = {
  get: <TResponse>(path: string) => request<TResponse>(path, { method: 'GET' }),
  post: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <TResponse, TBody = unknown>(path: string, body: TBody) =>
    request<TResponse>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
  postForm: <TResponse>(path: string, formData: FormData) => request<TResponse>(path, { method: 'POST', body: formData }),
}
