import { httpClient } from '@/shared/api/httpClient'
import type { ChangePasswordInput, ChangePasswordResponse, Profile, UpdateProfileInput } from '../types'

export const profileApi = {
  get: () => httpClient.get<Profile>('/profile'),
  update: (input: UpdateProfileInput) => httpClient.put<Profile>('/profile', input),
  changePassword: (input: ChangePasswordInput) =>
    httpClient.put<ChangePasswordResponse>('/profile/password', input),
}
