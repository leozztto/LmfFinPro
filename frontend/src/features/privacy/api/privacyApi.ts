import { httpClient } from '@/shared/api/httpClient'
import type { AccountDeletionPreview } from '../types'

export const privacyApi = {
  /** ZIP com os dados do titular (dados.json, anexos, foto e LEIA-ME). */
  exportData: () => httpClient.getBlob('/privacy/export'),
  getDeletionPreview: () => httpClient.get<AccountDeletionPreview>('/privacy/account-deletion-preview'),
  /** Definitivo; a senha confirma que quem pede é a pessoa dona da conta. */
  deleteAccount: (password: string) => httpClient.delete('/privacy/account', { password }),
}
