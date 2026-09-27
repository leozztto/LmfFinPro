import { useQuery } from '@tanstack/react-query'
import { calendarApi } from '../api/calendarApi'

export const CALENDAR_QUERY_KEY = ['calendar'] as const

export function useCalendar(month: string, includePaid: boolean) {
  return useQuery({
    queryKey: [...CALENDAR_QUERY_KEY, month, includePaid],
    queryFn: () => calendarApi.get(month, includePaid),
  })
}
