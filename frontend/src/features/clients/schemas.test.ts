import { describe, expect, it } from 'vitest'
import { clientSchema } from './schemas'

function validClient(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    name: 'Empresa X',
    email: 'contato@empresax.com',
    phone: '11987654321',
    documentType: 'CPF' as const,
    documentNumber: '52998224725',
    workType: 'PJ' as const,
    notes: '',
    color: '',
    active: true,
    ...overrides,
  }
}

describe('clientSchema', () => {
  it('accepts fully valid input with CPF', () => {
    expect(clientSchema.safeParse(validClient()).success).toBe(true)
  })

  it('accepts fully valid input with CNPJ', () => {
    const result = clientSchema.safeParse(
      validClient({ documentType: 'CNPJ', documentNumber: '11444777000161' }),
    )

    expect(result.success).toBe(true)
  })

  it('rejects an empty name', () => {
    expect(clientSchema.safeParse(validClient({ name: '   ' })).success).toBe(false)
  })

  it('rejects a missing email', () => {
    expect(clientSchema.safeParse(validClient({ email: '' })).success).toBe(false)
  })

  it('rejects a malformed email', () => {
    expect(clientSchema.safeParse(validClient({ email: 'not-an-email' })).success).toBe(false)
  })

  it('rejects a phone with the wrong number of digits', () => {
    expect(clientSchema.safeParse(validClient({ phone: '119876' })).success).toBe(false)
  })

  it('rejects an empty phone', () => {
    expect(clientSchema.safeParse(validClient({ phone: '' })).success).toBe(false)
  })

  it('accepts a formatted phone as long as the digit count is right', () => {
    expect(clientSchema.safeParse(validClient({ phone: '(11) 98765-4321' })).success).toBe(true)
  })

  it('rejects a missing work type', () => {
    expect(clientSchema.safeParse(validClient({ workType: '' })).success).toBe(false)
  })

  it('rejects an invalid CPF', () => {
    expect(clientSchema.safeParse(validClient({ documentNumber: '11111111111' })).success).toBe(false)
  })

  it('rejects a missing document type', () => {
    expect(clientSchema.safeParse(validClient({ documentType: '' })).success).toBe(false)
  })

  it('rejects a missing document number', () => {
    expect(clientSchema.safeParse(validClient({ documentNumber: '' })).success).toBe(false)
  })

  it('rejects an invalid CNPJ', () => {
    const result = clientSchema.safeParse(
      validClient({ documentType: 'CNPJ', documentNumber: '11111111111111' }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects notes longer than 1000 characters', () => {
    expect(clientSchema.safeParse(validClient({ notes: 'a'.repeat(1001) })).success).toBe(false)
  })

  it('accepts notes at exactly the 1000 character limit', () => {
    expect(clientSchema.safeParse(validClient({ notes: 'a'.repeat(1000) })).success).toBe(true)
  })

  it('accepts a client with notes entirely omitted', () => {
    const { notes: _notes, ...rest } = validClient()
    expect(clientSchema.safeParse(rest).success).toBe(true)
  })
})
