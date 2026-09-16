import { useMutation } from '@tanstack/react-query'
import { useAuth } from '@/shared/auth/AuthContext'
import type { LoginPayload } from '@/shared/auth/types'

export function useLogin() {
  const { login } = useAuth()
  return useMutation({
    mutationFn: (payload: LoginPayload) => login(payload),
  })
}
