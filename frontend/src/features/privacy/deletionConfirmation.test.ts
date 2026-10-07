import { describe, expect, it } from 'vitest'
import { DELETE_CONFIRMATION_WORD, canConfirmDeletion } from './deletionConfirmation'

const ready = { password: 'senha12345', confirmation: DELETE_CONFIRMATION_WORD, blocked: false, loadingPreview: false }

describe('canConfirmDeletion', () => {
  it('enables with the password, the confirmation word and no blockers', () => {
    expect(canConfirmDeletion(ready)).toBe(true)
  })

  it('accepts the word in any case and with surrounding spaces', () => {
    expect(canConfirmDeletion({ ...ready, confirmation: '  excluir ' })).toBe(true)
  })

  it('stays disabled without a password', () => {
    expect(canConfirmDeletion({ ...ready, password: '' })).toBe(false)
  })

  it('stays disabled with a wrong or partial confirmation word', () => {
    expect(canConfirmDeletion({ ...ready, confirmation: '' })).toBe(false)
    expect(canConfirmDeletion({ ...ready, confirmation: 'EXCLU' })).toBe(false)
    expect(canConfirmDeletion({ ...ready, confirmation: 'APAGAR' })).toBe(false)
  })

  it('stays disabled while the server reports a blocker', () => {
    expect(canConfirmDeletion({ ...ready, blocked: true })).toBe(false)
  })

  it('stays disabled until the preview has loaded', () => {
    expect(canConfirmDeletion({ ...ready, loadingPreview: true })).toBe(false)
  })
})
