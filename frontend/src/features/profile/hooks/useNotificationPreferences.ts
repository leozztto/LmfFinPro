import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { profileApi } from '../api/profileApi'

export const NOTIFICATION_PREFERENCES_QUERY_KEY = ['profile', 'notifications'] as const

export function useNotificationPreferences() {
  return useQuery({ queryKey: NOTIFICATION_PREFERENCES_QUERY_KEY, queryFn: profileApi.getNotifications })
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.updateNotifications,
    onSuccess: (preferences) => queryClient.setQueryData(NOTIFICATION_PREFERENCES_QUERY_KEY, preferences),
  })
}
