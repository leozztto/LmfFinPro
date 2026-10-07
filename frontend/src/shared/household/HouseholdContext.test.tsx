// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Household } from '@/features/households/types'
import { HouseholdProvider, useHousehold } from './HouseholdContext'
import { getActiveHouseholdId, restoreActiveHousehold, setActiveHouseholdId } from './householdStorage'

const households: Household[] = [
  { id: 1, name: 'Pessoal', type: 'PERSONAL', role: 'OWNER' },
  { id: 7, name: 'Casa', type: 'SHARED', role: 'MEMBER' },
]

const auth = vi.hoisted(() => ({ session: null as { token: string } | null }))

vi.mock('@/shared/auth/AuthContext', () => ({ useAuth: () => ({ session: auth.session }) }))
vi.mock('@/shared/toast/ToastContext', () => ({ useToast: () => ({ showToast: vi.fn() }) }))
vi.mock('@/features/households/hooks/useHouseholds', () => ({
  HOUSEHOLDS_QUERY_KEY: ['households'],
  useHouseholdList: (enabled: boolean) => ({ data: enabled ? households : undefined, isLoading: false }),
}))

function Probe() {
  const { active, activeKey } = useHousehold()
  return <p data-testid="active">{`${active?.name ?? 'none'}|${activeKey}`}</p>
}

function tree(client: QueryClient) {
  return (
    <QueryClientProvider client={client}>
      <HouseholdProvider>
        <Probe />
      </HouseholdProvider>
    </QueryClientProvider>
  )
}

function renderProvider() {
  const client = new QueryClient()
  const view = render(tree(client))
  return { ...view, rerenderProvider: () => view.rerender(tree(client)) }
}

describe('HouseholdProvider', () => {
  beforeEach(() => {
    localStorage.clear()
    auth.session = null
  })
  afterEach(cleanup)

  it('keeps the chosen shared group after a reload, while the session is still being restored', () => {
    setActiveHouseholdId(7)
    restoreActiveHousehold() // o reload: o módulo relê o grupo guardado
    const { rerenderProvider } = renderProvider()

    // Sessão ainda nula (cookie de refresh em andamento): o grupo escolhido não pode ser descartado,
    // senão a tela mostra o pessoal enquanto as requisições seguem com o header do grupo.
    expect(getActiveHouseholdId()).toBe(7)

    auth.session = { token: 'abc' }
    act(() => rerenderProvider())

    expect(screen.getByTestId('active').textContent).toBe('Casa|shared-7')
    expect(getActiveHouseholdId()).toBe(7)
  })

  it('goes back to the personal space once the session is gone and the group was cleared', () => {
    auth.session = { token: 'abc' }
    setActiveHouseholdId(7)
    const { rerenderProvider } = renderProvider()
    expect(screen.getByTestId('active').textContent).toBe('Casa|shared-7')

    // Logout: o clearSession limpa o módulo antes de a sessão virar nula.
    setActiveHouseholdId(null)
    auth.session = null
    act(() => rerenderProvider())

    expect(screen.getByTestId('active').textContent).toBe('none|personal')
  })
})
