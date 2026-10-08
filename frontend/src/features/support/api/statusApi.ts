import { httpClient } from '@/shared/api/httpClient'
import type { PlatformStatus } from '../types'

export const statusApi = {
  /** Público: a página de suporte também serve a quem não consegue entrar. */
  getStatus: () => httpClient.get<PlatformStatus>('/status'),
}
