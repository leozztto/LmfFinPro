import { useMutation, useQuery } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { getCurrentIsoDate } from '@/shared/format/date'
import { privacyApi } from '../api/privacyApi'

/** Baixa o ZIP com todos os dados da pessoa. */
export function useExportData() {
  return useMutation({
    mutationFn: privacyApi.exportData,
    onSuccess: (blob) => downloadBlob(blob, `finpro-meus-dados-${getCurrentIsoDate()}.zip`),
  })
}

/** Sempre atual: só busca quando a janela de exclusão abre e não guarda nada em cache. */
export function useDeletionPreview(enabled: boolean) {
  return useQuery({
    queryKey: ['privacy', 'deletion-preview'],
    queryFn: privacyApi.getDeletionPreview,
    enabled,
    gcTime: 0,
  })
}

export function useDeleteAccount() {
  return useMutation({ mutationFn: privacyApi.deleteAccount })
}
