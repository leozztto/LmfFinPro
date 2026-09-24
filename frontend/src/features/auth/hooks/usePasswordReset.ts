import { useMutation } from '@tanstack/react-query'
import { passwordResetApi } from '../api/passwordResetApi'

export function useForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => passwordResetApi.requestReset(email),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: ({ token, password }: { token: string; password: string }) =>
      passwordResetApi.resetPassword(token, password),
  })
}
