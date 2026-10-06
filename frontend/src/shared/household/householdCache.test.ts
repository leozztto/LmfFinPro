import { QueryClient, QueryObserver } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { isHouseholdScopedQuery, refreshHouseholdScopedQueries, removeHouseholdScopedQueries } from './householdCache'

describe('householdCache', () => {
  it('treats data queries as scoped to the active group, and the group list as global', () => {
    expect(isHouseholdScopedQuery(['accounts'])).toBe(true)
    expect(isHouseholdScopedQuery(['transactions', { page: 0 }])).toBe(true)
    expect(isHouseholdScopedQuery(['households'])).toBe(false)
    expect(isHouseholdScopedQuery(['households', 3, 'members'])).toBe(false)
  })

  it('removes the cached data of the previous group but keeps the group list', () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(['accounts'], [{ id: 1 }])
    queryClient.setQueryData(['transactions', { page: 0 }], [])
    queryClient.setQueryData(['households'], [{ id: 10 }])
    queryClient.setQueryData(['households', 10, 'members'], [])

    removeHouseholdScopedQueries(queryClient)

    expect(queryClient.getQueryData(['accounts'])).toBeUndefined()
    expect(queryClient.getQueryData(['transactions', { page: 0 }])).toBeUndefined()
    expect(queryClient.getQueryData(['households'])).toEqual([{ id: 10 }])
    expect(queryClient.getQueryData(['households', 10, 'members'])).toEqual([])
  })

  describe('refreshHouseholdScopedQueries', () => {
    it('refetches what is on screen, discards what nobody shows and keeps the group list', async () => {
      const queryClient = new QueryClient()
      let accountCalls = 0
      const onScreen = new QueryObserver(queryClient, {
        queryKey: ['accounts'],
        queryFn: async () => ++accountCalls,
      })
      const unsubscribe = onScreen.subscribe(() => undefined)
      await onScreen.refetch()
      queryClient.setQueryData(['dashboard'], { stale: true })
      queryClient.setQueryData(['households'], [{ id: 10 }])
      const callsBefore = accountCalls

      await refreshHouseholdScopedQueries(queryClient)

      expect(accountCalls).toBeGreaterThan(callsBefore)
      expect(queryClient.getQueryData(['dashboard'])).toBeUndefined()
      expect(queryClient.getQueryData(['households'])).toEqual([{ id: 10 }])
      unsubscribe()
    })
  })
})
