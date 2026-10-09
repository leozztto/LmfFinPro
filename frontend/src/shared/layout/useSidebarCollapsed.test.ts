// @vitest-environment jsdom
import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SIDEBAR_COLLAPSED_KEY, useSidebarCollapsed } from './useSidebarCollapsed'

beforeEach(() => localStorage.clear())
afterEach(() => vi.restoreAllMocks())

describe('useSidebarCollapsed', () => {
  it('starts open when nothing was chosen yet', () => {
    const { result } = renderHook(() => useSidebarCollapsed())

    expect(result.current.collapsed).toBe(false)
  })

  it('toggles and remembers the choice for the next visit', () => {
    const { result, unmount } = renderHook(() => useSidebarCollapsed())

    act(() => result.current.toggle())
    expect(result.current.collapsed).toBe(true)
    expect(localStorage.getItem(SIDEBAR_COLLAPSED_KEY)).toBe('1')

    unmount()
    const again = renderHook(() => useSidebarCollapsed())
    expect(again.result.current.collapsed).toBe(true)

    act(() => again.result.current.toggle())
    expect(again.result.current.collapsed).toBe(false)
    expect(localStorage.getItem(SIDEBAR_COLLAPSED_KEY)).toBe('0')
  })

  it('works without storage, only for the current visit', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })
    const { result } = renderHook(() => useSidebarCollapsed())

    expect(result.current.collapsed).toBe(false)
    act(() => result.current.toggle())
    expect(result.current.collapsed).toBe(true)
  })
})
