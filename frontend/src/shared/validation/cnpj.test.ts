import { describe, expect, it } from 'vitest'
import { isValidCnpj } from './cnpj'

describe('isValidCnpj', () => {
  it('accepts a valid CNPJ', () => {
    expect(isValidCnpj('11444777000161')).toBe(true)
  })

  it('accepts a valid CNPJ with formatting characters', () => {
    expect(isValidCnpj('11.444.777/0001-61')).toBe(true)
  })

  it('rejects a CNPJ with wrong check digits', () => {
    expect(isValidCnpj('11444777000100')).toBe(false)
  })

  it('rejects a CNPJ with all digits equal', () => {
    expect(isValidCnpj('11111111111111')).toBe(false)
  })

  it('rejects a CNPJ with the wrong length', () => {
    expect(isValidCnpj('123456789')).toBe(false)
  })

  it('rejects an empty string', () => {
    expect(isValidCnpj('')).toBe(false)
  })

  it('accepts a valid CNPJ whose check digit computes to zero (remainder < 2)', () => {
    expect(isValidCnpj('01234568350203')).toBe(true)
  })
})
