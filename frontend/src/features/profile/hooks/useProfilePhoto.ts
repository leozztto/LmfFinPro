import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { profileApi } from '../api/profileApi'
import { PROFILE_QUERY_KEY, useProfile } from './useProfile'

const PHOTO_QUERY_KEY = [...PROFILE_QUERY_KEY, 'photo'] as const

/**
 * URL (blob) da foto do usuário logado, ou null sem foto (que é opcional). A imagem exige o token,
 * então não dá para apontar um <img> direto para a API: baixa-se o arquivo e cria-se uma URL
 * local, liberada quando deixa de ser usada.
 */
export function useProfilePhotoUrl(): string | null {
  const { data: profile } = useProfile()
  const hasPhoto = profile?.hasPhoto ?? false

  const { data: url } = useQuery({
    queryKey: PHOTO_QUERY_KEY,
    queryFn: async () => URL.createObjectURL(await profileApi.getPhoto()),
    enabled: hasPhoto,
    staleTime: Infinity,
    gcTime: 0,
  })

  useEffect(() => {
    if (!url) return
    return () => URL.revokeObjectURL(url)
  }, [url])

  return hasPhoto ? (url ?? null) : null
}

/** Envia/troca a foto; o perfil atualizado (hasPhoto) já vem na resposta. */
export function useUploadProfilePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.uploadPhoto,
    onSuccess: (profile) => {
      queryClient.setQueryData(PROFILE_QUERY_KEY, profile)
      void queryClient.invalidateQueries({ queryKey: PHOTO_QUERY_KEY })
    },
  })
}

export function useRemoveProfilePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.removePhoto,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: PHOTO_QUERY_KEY })
      void queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY, exact: true })
    },
  })
}
