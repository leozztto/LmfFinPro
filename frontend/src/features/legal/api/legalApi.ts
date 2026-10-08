import { httpClient } from '@/shared/api/httpClient'
import type { ConsentStatus, LegalVersions } from '../types'

export const legalApi = {
  /** Público: o cadastro precisa das versões antes de existir uma sessão. */
  getVersions: () => httpClient.get<LegalVersions>('/legal/versions'),
  getConsentStatus: () => httpClient.get<ConsentStatus>('/consents'),
  acceptConsent: (versions: LegalVersions) => httpClient.post<ConsentStatus, LegalVersions>('/consents', versions),
}
