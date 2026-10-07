import { describe, expect, it } from 'vitest'
import { canDeleteRecord, deleteBlockedReason } from './recordAuthorship'

describe('canDeleteRecord', () => {
  it('lets the author delete', () => {
    expect(canDeleteRecord(7, 7)).toBe(true)
  })

  it('blocks anyone else', () => {
    expect(canDeleteRecord(8, 7)).toBe(false)
  })

  it('lets any member delete when the author is unknown (created by the system)', () => {
    expect(canDeleteRecord(null, 7)).toBe(true)
    expect(canDeleteRecord(undefined, 7)).toBe(true)
  })
})

describe('deleteBlockedReason', () => {
  it('is null when the user can delete', () => {
    expect(deleteBlockedReason(7, 'Ana', 7, 'lançamento')).toBeNull()
    expect(deleteBlockedReason(null, null, 7, 'transferência')).toBeNull()
  })

  it('names the author in the explanation', () => {
    expect(deleteBlockedReason(8, 'Bia', 7, 'lançamento')).toBe(
      'Só Bia, que criou este lançamento, pode excluí-lo.',
    )
    expect(deleteBlockedReason(8, 'Bia', 7, 'transferência')).toBe(
      'Só Bia, que criou esta transferência, pode excluí-la.',
    )
  })

  it('still explains when the author name is unavailable', () => {
    expect(deleteBlockedReason(8, null, 7, 'lançamento')).toBe(
      'Só a pessoa que criou este lançamento, pode excluí-lo.',
    )
  })
})
