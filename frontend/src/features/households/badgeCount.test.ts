import { describe, expect, it } from 'vitest'
import { formatBadgeCount, pendingInvitesLabel } from './badgeCount'

describe('formatBadgeCount', () => {
  it('shows the number as is up to 9', () => {
    expect(formatBadgeCount(1)).toBe('1')
    expect(formatBadgeCount(9)).toBe('9')
  })

  it('caps larger numbers at 9+', () => {
    expect(formatBadgeCount(10)).toBe('9+')
    expect(formatBadgeCount(250)).toBe('9+')
  })
})

describe('pendingInvitesLabel', () => {
  it('uses the singular for one invite and the plural otherwise', () => {
    expect(pendingInvitesLabel(1)).toBe('1 convite pendente')
    expect(pendingInvitesLabel(3)).toBe('3 convites pendentes')
  })
})
