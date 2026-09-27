import { httpClient } from '@/shared/api/httpClient'
import type { Calendar } from '../types'

export const calendarApi = {
  get: (month: string, includePaid: boolean) =>
    httpClient.get<Calendar>(`/calendar?month=${month}&includePaid=${includePaid}`),
}
