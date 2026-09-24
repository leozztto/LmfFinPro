import { describe, expect, it } from 'vitest'
import {
  documentTypeForTaxRegime,
  forgotPasswordSchema,
  loginSchema,
  registerSchema,
  resetPasswordSchema,
} from './schemas'

describe('loginSchema', () => {
  it('accepts valid credentials', () => {
    expect(loginSchema.safeParse({ email: 'ana@finpro.test', password: 'senha12345' }).success).toBe(true)
  })

  it('rejects a malformed email', () => {
    expect(loginSchema.safeParse({ email: 'not-an-email', password: 'senha12345' }).success).toBe(false)
  })

  it('rejects an empty password', () => {
    expect(loginSchema.safeParse({ email: 'ana@finpro.test', password: '' }).success).toBe(false)
  })
})

describe('documentTypeForTaxRegime', () => {
  it('requires CNPJ for legal-entity regimes', () => {
    expect(documentTypeForTaxRegime('MEI')).toBe('CNPJ')
    expect(documentTypeForTaxRegime('SIMPLES_NACIONAL')).toBe('CNPJ')
    expect(documentTypeForTaxRegime('LUCRO_PRESUMIDO')).toBe('CNPJ')
  })

  it('requires CPF for individual regimes', () => {
    expect(documentTypeForTaxRegime('AUTONOMO')).toBe('CPF')
    expect(documentTypeForTaxRegime('OUTRO')).toBe('CPF')
  })
})

function validRegisterPayload(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    name: 'Ana Freelancer',
    email: 'ana@finpro.test',
    taxRegime: 'AUTONOMO',
    documentNumber: '52998224725',
    phone: '11987654321',
    address: {
      zipCode: '01310100',
      street: 'Avenida Paulista',
      number: '1000',
      complement: '',
      neighborhood: 'Bela Vista',
      city: 'São Paulo',
      state: 'SP',
    },
    password: 'senha12345',
    confirmPassword: 'senha12345',
    acceptedTerms: true,
    ...overrides,
  }
}

describe('registerSchema', () => {
  it('accepts fully valid input', () => {
    expect(registerSchema.safeParse(validRegisterPayload()).success).toBe(true)
  })

  it('rejects a name without a surname', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ name: 'Ana' })).success).toBe(false)
  })

  it('rejects an empty name', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ name: '   ' })).success).toBe(false)
  })

  it('rejects an empty email', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ email: '' })).success).toBe(false)
  })

  it('rejects a malformed email', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ email: 'not-an-email' })).success).toBe(false)
  })

  it('rejects an empty document number', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ documentNumber: '' })).success).toBe(false)
  })

  it('rejects an invalid CNPJ when the regime requires CNPJ', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ taxRegime: 'MEI', documentNumber: '11111111111111' }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects an empty confirm password', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ confirmPassword: '' })).success).toBe(false)
  })

  it('rejects an empty zip code', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, zipCode: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a CPF that does not match the AUTONOMO regime (should be CPF but is CNPJ-shaped)', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ taxRegime: 'AUTONOMO', documentNumber: '11444777000161' }),
    )

    expect(result.success).toBe(false)
  })

  it('accepts a CNPJ when the regime is MEI', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ taxRegime: 'MEI', documentNumber: '11444777000161' }),
    )

    expect(result.success).toBe(true)
  })

  it('rejects a missing tax regime', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ taxRegime: '' })).success).toBe(false)
  })

  it('rejects a password shorter than 8 characters', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ password: 'abc123', confirmPassword: 'abc123' }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects mismatched password confirmation', () => {
    const result = registerSchema.safeParse(validRegisterPayload({ confirmPassword: 'outrasenha123' }))

    expect(result.success).toBe(false)
  })

  it('rejects when terms are not accepted', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ acceptedTerms: false })).success).toBe(false)
  })

  it('rejects a zip code with the wrong number of digits', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, zipCode: '123' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a missing street', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, street: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a missing address number', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, number: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a missing neighborhood', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, neighborhood: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a missing city', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, city: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('rejects a missing state', () => {
    const result = registerSchema.safeParse(
      validRegisterPayload({ address: { ...validRegisterPayload().address, state: '' } }),
    )

    expect(result.success).toBe(false)
  })

  it('accepts an optional phone left blank', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ phone: '' })).success).toBe(true)
  })

  it('rejects a phone with the wrong number of digits when provided', () => {
    expect(registerSchema.safeParse(validRegisterPayload({ phone: '123' })).success).toBe(false)
  })
})

describe('forgotPasswordSchema', () => {
  it('accepts a valid email', () => {
    expect(forgotPasswordSchema.safeParse({ email: 'ana@finpro.test' }).success).toBe(true)
  })

  it('rejects a malformed email', () => {
    expect(forgotPasswordSchema.safeParse({ email: 'ana' }).success).toBe(false)
  })
})

describe('resetPasswordSchema', () => {
  it('accepts matching passwords with at least 8 characters', () => {
    expect(resetPasswordSchema.safeParse({ password: 'novaSenha1', confirmPassword: 'novaSenha1' }).success).toBe(true)
  })

  it('rejects a password shorter than 8 characters', () => {
    expect(resetPasswordSchema.safeParse({ password: 'curta', confirmPassword: 'curta' }).success).toBe(false)
  })

  it('rejects when confirmation does not match', () => {
    const result = resetPasswordSchema.safeParse({ password: 'novaSenha1', confirmPassword: 'outraSenha1' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path).toEqual(['confirmPassword'])
  })
})
