import { describe, expect, it } from 'vitest'
import { isValidCpf } from './cpf'

describe('isValidCpf', () => {
  it('accepts a valid CPF', () => {
    expect(isValidCpf('52998224725')).toBe(true)
  })

  it('accepts a valid CPF with formatting characters', () => {
    expect(isValidCpf('529.982.247-25')).toBe(true)
  })

  it('rejects a CPF with wrong check digits', () => {
    expect(isValidCpf('52998224700')).toBe(false)
  })

  it('rejects a CPF with all digits equal', () => {
    expect(isValidCpf('11111111111')).toBe(false)
  })

  it('rejects a CPF with the wrong length', () => {
    expect(isValidCpf('123456789')).toBe(false)
  })

  it('rejects an empty string', () => {
    expect(isValidCpf('')).toBe(false)
  })
})
