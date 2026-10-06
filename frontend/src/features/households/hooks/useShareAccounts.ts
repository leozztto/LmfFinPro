import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { accountsApi } from '@/features/accounts/api/accountsApi'
import { refreshHouseholdScopedQueries } from '@/shared/household/householdCache'
import { householdsApi } from '../api/householdsApi'
import { HOUSEHOLDS_QUERY_KEY } from './useHouseholds'

/** Contas do espaço pessoal, as candidatas a serem compartilhadas. Vale mesmo vendo outro grupo. */
export function usePersonalAccounts(personalHouseholdId: number | null, enabled: boolean) {
  return useQuery({
    queryKey: ['personal-accounts', personalHouseholdId],
    queryFn: () => accountsApi.listOfHousehold(personalHouseholdId as number),
    enabled: enabled && personalHouseholdId !== null,
  })
}

/**
 * Compartilhar ou descompartilhar muda de dono as contas e o histórico delas (e recria categorias,
 * clientes e tags do outro lado), então todo dado em cache deixa de valer. O que está na tela é
 * recarregado na hora.
 */
async function refreshAfterMove(queryClient: QueryClient): Promise<void> {
  await refreshHouseholdScopedQueries(queryClient)
  void queryClient.invalidateQueries({ queryKey: HOUSEHOLDS_QUERY_KEY })
}

/** Compartilha com um grupo já conhecido (o cartão do grupo, em Configurações). */
export function useShareAccounts(householdId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (accountIds: number[]) => householdsApi.shareAccounts(householdId, accountIds),
    onSuccess: () => refreshAfterMove(queryClient),
  })
}

/** Compartilha com um grupo escolhido na hora (o diálogo de uma conta, na tela de Contas). */
export function useShareAccountsToGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ householdId, accountIds }: { householdId: number; accountIds: number[] }) =>
      householdsApi.shareAccounts(householdId, accountIds),
    onSuccess: () => refreshAfterMove(queryClient),
  })
}

/** O inverso: devolve contas do grupo ao espaço pessoal de quem pede, com todo o histórico. */
export function useUnshareAccounts() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ householdId, accountIds }: { householdId: number; accountIds: number[] }) =>
      householdsApi.unshareAccounts(householdId, accountIds),
    onSuccess: () => refreshAfterMove(queryClient),
  })
}
