import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/shared/auth/AuthContext'
import { useToast } from '@/shared/toast/ToastContext'
import { HOUSEHOLDS_QUERY_KEY, useHouseholdList } from '@/features/households/hooks/useHouseholds'
import type { Household } from '@/features/households/types'
import { removeHouseholdScopedQueries } from './householdCache'
import {
  HOUSEHOLD_FORBIDDEN_EVENT,
  getActiveHouseholdId,
  householdEvents,
  setActiveHouseholdId,
} from './householdStorage'

interface HouseholdContextValue {
  /** Todos os grupos da pessoa: o espaço pessoal e os compartilhados. */
  households: Household[]
  personal: Household | null
  sharedHouseholds: Household[]
  /** O grupo que as telas estão mostrando agora (o pessoal, se nenhum compartilhado foi escolhido). */
  active: Household | null
  /** true quando os dados na tela são de um grupo compartilhado. */
  isShared: boolean
  /** Muda a cada troca de grupo; usado como `key` para recarregar a tela atual do zero. */
  activeKey: string
  loading: boolean
  /** Alterna o grupo ativo; `null` (ou o id do espaço pessoal) volta para os dados pessoais. */
  switchTo: (householdId: number | null) => void
}

const HouseholdContext = createContext<HouseholdContextValue | undefined>(undefined)

export function HouseholdProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const { data, isLoading } = useHouseholdList(session !== null)
  const households = useMemo(() => data ?? [], [data])
  const [activeId, setActiveId] = useState<number | null>(() => getActiveHouseholdId())

  const personal = households.find((household) => household.type === 'PERSONAL') ?? null
  const sharedHouseholds = useMemo(() => households.filter((household) => household.type === 'SHARED'), [households])

  const switchTo = useCallback(
    (householdId: number | null) => {
      const next = householdId !== null && householdId !== personal?.id ? householdId : null
      // O módulo vem primeiro: as telas que remontam em seguida já precisam mandar o header novo.
      setActiveHouseholdId(next)
      setActiveId(next)
      removeHouseholdScopedQueries(queryClient)
    },
    [personal?.id, queryClient],
  )

  // Sair (ou a sessão cair) limpa o grupo no módulo (clearSession); o estado do React acompanha o
  // que o módulo tem. Zerar direto aqui quebrava o reload: a sessão começa nula enquanto é
  // retomada pelo cookie, o grupo guardado seguia valendo nas requisições e a tela mostrava o
  // espaço pessoal.
  useEffect(() => {
    if (session === null) setActiveId(getActiveHouseholdId())
  }, [session])

  // O grupo guardado pode não valer mais (a pessoa foi removida dele, ou ele deixou de existir).
  useEffect(() => {
    if (activeId === null || isLoading || !data) return
    if (!data.some((household) => household.id === activeId && household.type === 'SHARED')) {
      switchTo(null)
      showToast('Você não participa mais desse grupo. Voltamos para os seus dados pessoais.')
    }
  }, [activeId, data, isLoading, showToast, switchTo])

  // 403 com um grupo escolhido: confere a lista; se a pessoa saiu do grupo, o efeito acima a devolve
  // ao espaço pessoal.
  useEffect(() => {
    function handleForbidden() {
      void queryClient.invalidateQueries({ queryKey: HOUSEHOLDS_QUERY_KEY })
    }
    householdEvents.addEventListener(HOUSEHOLD_FORBIDDEN_EVENT, handleForbidden)
    return () => householdEvents.removeEventListener(HOUSEHOLD_FORBIDDEN_EVENT, handleForbidden)
  }, [queryClient])

  const active = sharedHouseholds.find((household) => household.id === activeId) ?? personal

  const value = useMemo<HouseholdContextValue>(
    () => ({
      households,
      personal,
      sharedHouseholds,
      active,
      isShared: active?.type === 'SHARED',
      activeKey: active?.type === 'SHARED' ? `shared-${active.id}` : 'personal',
      loading: isLoading,
      switchTo,
    }),
    [households, personal, sharedHouseholds, active, isLoading, switchTo],
  )

  return <HouseholdContext.Provider value={value}>{children}</HouseholdContext.Provider>
}

export function useHousehold(): HouseholdContextValue {
  const context = useContext(HouseholdContext)
  if (!context) {
    throw new Error('useHousehold deve ser usado dentro de um HouseholdProvider')
  }
  return context
}
