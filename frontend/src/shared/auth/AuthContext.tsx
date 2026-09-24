import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { httpClient } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { SESSION_EXPIRED_EVENT, authEvents, clearSession, loadSession, saveSession } from './authStorage'
import type { AuthSession, LoginPayload, RegisterPayload } from './types'

interface AuthResponseBody {
  token: string
  userId: number
  name: string
  email: string
}

interface AuthContextValue {
  session: AuthSession | null
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => void
  /** Atualiza a sessão salva (ex: nome/e-mail após editar o perfil, token novo após trocar a senha). */
  updateSession: (changes: Partial<AuthSession>) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession())
  const { showToast } = useToast()

  // O httpClient já limpou a sessão do localStorage (ela já tinha morrido no servidor); aqui só
  // sincronizamos o estado do React, o que faz o ProtectedRoute mandar pra /login sozinho.
  useEffect(() => {
    function handleSessionExpired() {
      setSession(null)
      showToast('Sua sessão expirou. Faça login novamente.')
    }
    authEvents.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
    return () => authEvents.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [showToast])

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

  const value = useMemo(() => ({ session, login, register, logout, updateSession }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
