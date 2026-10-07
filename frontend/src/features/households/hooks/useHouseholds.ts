import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { householdsApi } from '../api/householdsApi'

/** Raiz de tudo o que é gestão de grupos (lista, membros, convites). Fica fora da limpeza de cache
 *  que acontece ao trocar de grupo: a lista de grupos vale para qualquer um deles. */
export const HOUSEHOLDS_QUERY_KEY = ['households'] as const

export function useHouseholdList(enabled = true) {
  return useQuery({ queryKey: HOUSEHOLDS_QUERY_KEY, queryFn: householdsApi.list, enabled })
}

export function useHouseholdMembers(householdId: number) {
  return useQuery({
    queryKey: [...HOUSEHOLDS_QUERY_KEY, householdId, 'members'],
    queryFn: () => householdsApi.members(householdId),
  })
}

/** Só o dono enxerga os convites pendentes; para os demais a consulta nem é feita. */
export function useHouseholdInvites(householdId: number, enabled: boolean) {
  return useQuery({
    queryKey: [...HOUSEHOLDS_QUERY_KEY, householdId, 'invites'],
    queryFn: () => householdsApi.invites(householdId),
    enabled,
  })
}

function useInvalidateHouseholds() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: HOUSEHOLDS_QUERY_KEY })
}

export function useCreateHousehold() {
  const invalidate = useInvalidateHouseholds()
  return useMutation({ mutationFn: (name: string) => householdsApi.create(name), onSuccess: invalidate })
}

export function useInviteMember(householdId: number) {
  const invalidate = useInvalidateHouseholds()
  return useMutation({ mutationFn: (email: string) => householdsApi.invite(householdId, email), onSuccess: invalidate })
}

export function useRevokeInvite(householdId: number) {
  const invalidate = useInvalidateHouseholds()
  return useMutation({
    mutationFn: (inviteId: number) => householdsApi.revokeInvite(householdId, inviteId),
    onSuccess: invalidate,
  })
}

export function useRemoveMember(householdId: number) {
  const invalidate = useInvalidateHouseholds()
  return useMutation({
    mutationFn: (userId: number) => householdsApi.removeMember(householdId, userId),
    onSuccess: invalidate,
  })
}

export function useLeaveHousehold(householdId: number) {
  const invalidate = useInvalidateHouseholds()
  return useMutation({ mutationFn: () => householdsApi.leave(householdId), onSuccess: invalidate })
}

export function useTransferOwnership(householdId: number) {
  const invalidate = useInvalidateHouseholds()
  return useMutation({
    mutationFn: (newOwnerUserId: number) => householdsApi.transferOwnership(householdId, newOwnerUserId),
    onSuccess: invalidate,
  })
}

export function useAcceptInvite() {
  const invalidate = useInvalidateHouseholds()
  return useMutation({ mutationFn: (token: string) => householdsApi.acceptInvite(token), onSuccess: invalidate })
}

/**
 * Convites pendentes endereçados ao e-mail da conta. Fica sob a raiz de grupos, então qualquer
 * ação sobre grupos o recarrega. Confere de tempos em tempos: o convite pode chegar com o app aberto.
 */
export function useReceivedInvites() {
  return useQuery({
    queryKey: [...HOUSEHOLDS_QUERY_KEY, 'received-invites'],
    queryFn: householdsApi.receivedInvites,
    refetchInterval: 60_000,
  })
}

export function useAcceptReceivedInvite() {
  const invalidate = useInvalidateHouseholds()
  return useMutation({
    mutationFn: (inviteId: number) => householdsApi.acceptReceivedInvite(inviteId),
    onSuccess: invalidate,
  })
}

export function useDeclineReceivedInvite() {
  const invalidate = useInvalidateHouseholds()
  return useMutation({
    mutationFn: (inviteId: number) => householdsApi.declineReceivedInvite(inviteId),
    onSuccess: invalidate,
  })
}
