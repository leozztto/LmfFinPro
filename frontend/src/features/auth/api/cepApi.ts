import { httpClient } from '@/shared/api/httpClient'
import type { CepAddress } from '@/shared/auth/types'

export const cepApi = {
  lookup: (zipCode: string) => httpClient.get<CepAddress>(`/cep/${zipCode}`),
}
