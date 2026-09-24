import { httpClient } from '@/shared/api/httpClient'

export const passwordResetApi = {
  requestReset: (email: string) => httpClient.post<void>('/auth/forgot-password', { email }),
  resetPassword: (token: string, password: string) =>
    httpClient.post<void>('/auth/reset-password', { token, password }),
}
