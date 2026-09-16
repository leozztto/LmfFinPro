export interface AuthSession {
  token: string
  userId: number
  name: string
  email: string
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
  taxRegime?: string
}

export interface LoginPayload {
  email: string
  password: string
}
