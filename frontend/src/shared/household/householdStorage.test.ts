import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ACTIVE_HOUSEHOLD_KEY,
  clearActiveHousehold,
  getActiveHouseholdId,
  restoreActiveHousehold,
  setActiveHouseholdId,
} from './householdStorage'

function createLocalStorageMock() {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => {
      store[key] = value
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      store = {}
    },
  }
}

describe('householdStorage', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', createLocalStorageMock())
    restoreActiveHousehold()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('defaults to the personal space (null) when nothing is stored', () => {
    expect(getActiveHouseholdId()).toBeNull()
  })

  it('remembers the chosen group in memory and in the localStorage', () => {
    setActiveHouseholdId(7)

    expect(getActiveHouseholdId()).toBe(7)
    expect(localStorage.getItem(ACTIVE_HOUSEHOLD_KEY)).toBe('7')
  })

  it('restores the chosen group from the localStorage after a reload', () => {
    localStorage.setItem(ACTIVE_HOUSEHOLD_KEY, '12')

    expect(restoreActiveHousehold()).toBe(12)
    expect(getActiveHouseholdId()).toBe(12)
  })

  it('going back to the personal space removes the stored choice', () => {
    setActiveHouseholdId(7)

    setActiveHouseholdId(null)

    expect(getActiveHouseholdId()).toBeNull()
    expect(localStorage.getItem(ACTIVE_HOUSEHOLD_KEY)).toBeNull()
  })

  it('ignores a corrupted stored value', () => {
    for (const corrupted of ['abc', '-3', '0', '1.5']) {
      localStorage.setItem(ACTIVE_HOUSEHOLD_KEY, corrupted)
      expect(restoreActiveHousehold()).toBeNull()
    }
  })

  it('clearActiveHousehold forgets the group so it does not leak to the next login', () => {
    setActiveHouseholdId(7)

    clearActiveHousehold()

    expect(getActiveHouseholdId()).toBeNull()
    expect(localStorage.getItem(ACTIVE_HOUSEHOLD_KEY)).toBeNull()
  })

  it('keeps working in memory when the localStorage is unavailable', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('blocked')
      },
      setItem: () => {
        throw new Error('blocked')
      },
      removeItem: () => {
        throw new Error('blocked')
      },
    })

    expect(restoreActiveHousehold()).toBeNull()
    setActiveHouseholdId(5)
    expect(getActiveHouseholdId()).toBe(5)
    clearActiveHousehold()
    expect(getActiveHouseholdId()).toBeNull()
  })
})
