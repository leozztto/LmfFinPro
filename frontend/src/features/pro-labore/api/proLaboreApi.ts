import { httpClient } from '@/shared/api/httpClient'
import type { ProLaboreSettings, ProLaboreSummary } from '../types'

export const proLaboreApi = {
  summary: () => httpClient.get<ProLaboreSummary>('/pro-labore'),
  updateSettings: (settings: ProLaboreSettings) =>
    httpClient.put<ProLaboreSummary, ProLaboreSettings>('/pro-labore/settings', settings),
}
