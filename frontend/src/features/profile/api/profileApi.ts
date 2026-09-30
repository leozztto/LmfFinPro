import { httpClient } from '@/shared/api/httpClient'
import type {
  ChangePasswordInput,
  ChangePasswordResponse,
  NotificationPreferences,
  Profile,
  UpdateProfileInput,
} from '../types'

export const profileApi = {
  get: () => httpClient.get<Profile>('/profile'),
  update: (input: UpdateProfileInput) => httpClient.put<Profile>('/profile', input),
  changePassword: (input: ChangePasswordInput) =>
    httpClient.put<ChangePasswordResponse>('/profile/password', input),
  uploadPhoto: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return httpClient.putForm<Profile>('/profile/photo', formData)
  },
  removePhoto: () => httpClient.delete('/profile/photo'),
  getPhoto: () => httpClient.getBlob('/profile/photo'),
  getNotifications: () => httpClient.get<NotificationPreferences>('/profile/notifications'),
  updateNotifications: (input: NotificationPreferences) =>
    httpClient.put<NotificationPreferences>('/profile/notifications', input),
}
