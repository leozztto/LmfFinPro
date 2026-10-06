import type { QueryClient, QueryKey } from '@tanstack/react-query'
import { HOUSEHOLDS_QUERY_KEY } from '@/features/households/hooks/useHouseholds'

/**
 * As chaves de cache (['accounts'], ['transactions']...) não incluem o grupo, então o que está em
 * cache vale só para o grupo que estava ativo. Tudo é descartado ao trocar de grupo (e o que o
 * compartilhamento de contas move de dono), menos a lista de grupos, que vale para qualquer um.
 */
export function isHouseholdScopedQuery(queryKey: QueryKey): boolean {
  return queryKey[0] !== HOUSEHOLDS_QUERY_KEY[0]
}

export function removeHouseholdScopedQueries(queryClient: QueryClient): void {
  queryClient.removeQueries({ predicate: (query) => isHouseholdScopedQuery(query.queryKey) })
}

/**
 * Para quando os dados do grupo ativo mudam sem trocar de grupo (compartilhar ou descompartilhar uma
 * conta): o que ninguém está mostrando é descartado, e o que está na tela é recarregado na hora, para
 * a lista não ficar com a conta que acabou de sair (ou sem a que acabou de chegar).
 */
export function refreshHouseholdScopedQueries(queryClient: QueryClient): Promise<void> {
  queryClient.removeQueries({
    predicate: (query) => isHouseholdScopedQuery(query.queryKey) && query.getObserversCount() === 0,
  })
  return queryClient.invalidateQueries({ predicate: (query) => isHouseholdScopedQuery(query.queryKey) })
}
