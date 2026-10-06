import { httpClient } from '@/shared/api/httpClient'
import type {
  AccountSharingResult,
  Household,
  HouseholdInvite,
  HouseholdMember,
  ReceivedInvite,
} from '../types'

export const householdsApi = {
  list: () => httpClient.get<Household[]>('/households'),
  create: (name: string) => httpClient.post<Household, { name: string }>('/households', { name }),
  members: (householdId: number) => httpClient.get<HouseholdMember[]>(`/households/${householdId}/members`),
  invites: (householdId: number) => httpClient.get<HouseholdInvite[]>(`/households/${householdId}/invites`),
  invite: (householdId: number, email: string) =>
    httpClient.post<HouseholdInvite, { email: string }>(`/households/${householdId}/invites`, { email }),
  revokeInvite: (householdId: number, inviteId: number) =>
    httpClient.delete(`/households/${householdId}/invites/${inviteId}`),
  receivedInvites: () => httpClient.get<ReceivedInvite[]>('/households/invites/received'),
  acceptReceivedInvite: (inviteId: number) =>
    httpClient.post<Household, undefined>(`/households/invites/${inviteId}/accept`, undefined),
  declineReceivedInvite: (inviteId: number) =>
    httpClient.post<void, undefined>(`/households/invites/${inviteId}/decline`, undefined),
  acceptInvite: (token: string) =>
    httpClient.post<Household, { token: string }>('/households/invites/accept', { token }),
  removeMember: (householdId: number, userId: number) =>
    httpClient.delete(`/households/${householdId}/members/${userId}`),
  leave: (householdId: number) => httpClient.post<void, undefined>(`/households/${householdId}/leave`, undefined),
  transferOwnership: (householdId: number, newOwnerUserId: number) =>
    httpClient.put<void, { newOwnerUserId: number }>(`/households/${householdId}/owner`, { newOwnerUserId }),
  unshareAccounts: (householdId: number, accountIds: number[]) =>
    httpClient.post<AccountSharingResult, { accountIds: number[] }>(`/households/${householdId}/accounts/unshare`, {
      accountIds,
    }),
  shareAccounts: (householdId: number, accountIds: number[]) =>
    httpClient.post<AccountSharingResult, { accountIds: number[] }>(`/households/${householdId}/accounts/share`, {
      accountIds,
    }),
}
