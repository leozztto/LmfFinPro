import { describe, expect, it } from 'vitest'
import { householdSchema, inviteSchema } from './schemas'

describe('householdSchema', () => {
  it('accepts a normal name and trims the spaces around it', () => {
    const result = householdSchema.safeParse({ name: '  Família Silva ' })

    expect(result.success).toBe(true)
    if (result.success) expect(result.data.name).toBe('Família Silva')
  })

  it('rejects an empty or blank name', () => {
    expect(householdSchema.safeParse({ name: '' }).success).toBe(false)
    expect(householdSchema.safeParse({ name: '   ' }).success).toBe(false)
  })

  it('rejects a name longer than 150 characters', () => {
    expect(householdSchema.safeParse({ name: 'a'.repeat(151) }).success).toBe(false)
    expect(householdSchema.safeParse({ name: 'a'.repeat(150) }).success).toBe(true)
  })
})

describe('inviteSchema', () => {
  it('accepts a valid e-mail', () => {
    expect(inviteSchema.safeParse({ email: 'bia@finpro.test' }).success).toBe(true)
  })

  it('rejects an empty e-mail with the required message', () => {
    const result = inviteSchema.safeParse({ email: '' })

    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues[0].message).toBe('e-mail é obrigatório')
  })

  it('rejects a malformed e-mail', () => {
    const result = inviteSchema.safeParse({ email: 'bia@' })

    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues[0].message).toBe('e-mail inválido')
  })
})
