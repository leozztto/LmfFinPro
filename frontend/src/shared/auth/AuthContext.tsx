import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { httpClient } from '@/shared/api/httpClient'
import { clearSession, loadSession, saveSession } from './authStorage'
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
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession())

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

  const value = useMemo(() => ({ session, login, register, logout }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
