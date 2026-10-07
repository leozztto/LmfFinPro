import { describe, expect, it } from 'vitest'
import { isConsentScreenExempt } from './consentGate'

describe('isConsentScreenExempt', () => {
  it('keeps the privacy settings open so the person can export data or delete the account', () => {
    expect(isConsentScreenExempt('/configuracoes/privacidade')).toBe(true)
    expect(isConsentScreenExempt('/configuracoes/privacidade/')).toBe(true)
  })

  it('blocks every other screen of the app', () => {
    expect(isConsentScreenExempt('/')).toBe(false)
    expect(isConsentScreenExempt('/transacoes')).toBe(false)
    expect(isConsentScreenExempt('/configuracoes')).toBe(false)
    expect(isConsentScreenExempt('/configuracoes/senha')).toBe(false)
  })

  it('does not let a look-alike path through', () => {
    expect(isConsentScreenExempt('/configuracoes/privacidades')).toBe(false)
    expect(isConsentScreenExempt('/x/configuracoes/privacidade')).toBe(false)
  })
})
