import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/shared/auth/AuthContext'
import { profileApi } from '../api/profileApi'

export const PROFILE_QUERY_KEY = ['profile'] as const

export function useProfile() {
  return useQuery({ queryKey: PROFILE_QUERY_KEY, queryFn: profileApi.get })
}

/** Além de salvar, atualiza nome/e-mail da sessão (mostrados no topo da tela). */
export function useUpdateProfile() {
  const queryClient = useQueryClient()
  const { updateSession } = useAuth()
  return useMutation({
    mutationFn: profileApi.update,
    onSuccess: (profile) => {
      queryClient.setQueryData(PROFILE_QUERY_KEY, profile)
      updateSession({ name: profile.name, email: profile.email })
    },
  })
}

/** A troca encerra as outras sessões; a atual continua com o token novo devolvido pela API. */
export function useChangePassword() {
  const { updateSession } = useAuth()
  return useMutation({
    mutationFn: profileApi.changePassword,
    onSuccess: (response) => updateSession({ token: response.token }),
  })
}
