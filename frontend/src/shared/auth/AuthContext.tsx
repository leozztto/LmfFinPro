import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { endRemoteSession, httpClient, refreshSession } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import {
  SESSION_EXPIRED_EVENT,
  SESSION_KEY,
  authEvents,
  clearSession,
  getAccessToken,
  loadStoredUser,
  saveSession,
} from './authStorage'
import type { AuthSession, LoginPayload, RegisterPayload } from './types'

interface AuthResponseBody {
  token: string
  userId: number
  name: string
  email: string
}

interface AuthContextValue {
  session: AuthSession | null
  /** true enquanto o refresh token (cookie) é trocado por um access token após recarregar a página. */
  restoring: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => void
  /** Atualiza a sessão salva (ex: nome/e-mail após editar o perfil, token novo após trocar a senha). */
  updateSession: (changes: Partial<AuthSession>) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/** O access token só existe em memória, então após recarregar a página há um usuário lembrado no
 *  localStorage mas nenhum token: a sessão é retomada pelo cookie httpOnly de refresh. */
function needsRestore(): boolean {
  return loadStoredUser() !== null && getAccessToken() === null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => {
    const user = loadStoredUser()
    const token = getAccessToken()
    return user && token ? { ...user, token } : null
  })
  const [restoring, setRestoring] = useState<boolean>(needsRestore)
  const { showToast } = useToast()

  useEffect(() => {
    if (!restoring) return
    let cancelled = false
    refreshSession().then((refreshed) => {
      if (cancelled) return
      if (refreshed) {
        saveSession(refreshed)
        setSession(refreshed)
      } else {
        clearSession()
      }
      setRestoring(false)
    })
    return () => {
      cancelled = true
    }
  }, [restoring])

  // O httpClient já limpou a sessão (ela já tinha morrido no servidor); aqui só sincronizamos o
  // estado do React, o que faz o ProtectedRoute mandar pra /login sozinho.
  useEffect(() => {
    function handleSessionExpired() {
      setSession(null)
      showToast('Sua sessão expirou. Faça login novamente.')
    }
    authEvents.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
    return () => authEvents.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [showToast])

  // Sair em uma aba remove o usuário do localStorage: as outras abas acompanham.
  useEffect(() => {
    function handleStorage(event: StorageEvent) {
      if (event.key === SESSION_KEY && event.newValue === null) {
        clearSession()
        setSession(null)
      }
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  async function authenticate(path: string, payload: LoginPayload | RegisterPayload) {
    const response = await httpClient.post<AuthResponseBody>(path, payload)
    const nextSession: AuthSession = {
      token: response.token,
      userId: response.userId,
      name: response.name,
      email: response.email,
    }
    saveSession(nextSession)
    setSession(nextSession)
  }

  function login(payload: LoginPayload) {
    return authenticate('/auth/login', payload)
  }

  function register(payload: RegisterPayload) {
    return authenticate('/auth/register', payload)
  }

  function logout() {
    void endRemoteSession()
    clearSession()
    setSession(null)
  }

  function updateSession(changes: Partial<AuthSession>) {
    setSession((current) => {
      if (!current) return current
      const next = { ...current, ...changes }
      saveSession(next)
      return next
    })
  }

  const value = useMemo(() => ({ session, restoring, login, register, logout, updateSession }), [session, restoring])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
