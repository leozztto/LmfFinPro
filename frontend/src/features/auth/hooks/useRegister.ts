import { useMutation } from '@tanstack/react-query'
import { useAuth } from '@/shared/auth/AuthContext'
import type { RegisterPayload } from '@/shared/auth/types'

export function useRegister() {
  const { register } = useAuth()
  return useMutation({
    mutationFn: (payload: RegisterPayload) => register(payload),
  })
}
