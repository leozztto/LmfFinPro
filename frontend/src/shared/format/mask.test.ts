import { describe, expect, it } from 'vitest'
import { formatCep, formatCnpj, formatCpf, formatPhone, onlyDigits } from './mask'

describe('onlyDigits', () => {
  it('strips every non-digit character', () => {
    expect(onlyDigits('(11) 98765-4321')).toBe('11987654321')
  })

  it('returns empty string when there are no digits', () => {
    expect(onlyDigits('abc')).toBe('')
  })
})

describe('formatCpf', () => {
  it('formats a full 11-digit CPF', () => {
    expect(formatCpf('52998224725')).toBe('529.982.247-25')
  })

  it('formats partial input progressively', () => {
    expect(formatCpf('529982')).toBe('529.982')
    expect(formatCpf('5299822')).toBe('529.982.2')
  })

  it('truncates input longer than 11 digits', () => {
    expect(formatCpf('529982247259999')).toBe('529.982.247-25')
  })

  it('ignores pre-existing formatting characters in the input', () => {
    expect(formatCpf('529.982.247-25')).toBe('529.982.247-25')
  })
})

describe('formatCnpj', () => {
  it('formats a full 14-digit CNPJ', () => {
    expect(formatCnpj('11444777000161')).toBe('11.444.777/0001-61')
  })

  it('truncates input longer than 14 digits', () => {
    expect(formatCnpj('114447770001619999')).toBe('11.444.777/0001-61')
  })
})

describe('formatPhone', () => {
  it('formats a 10-digit landline number', () => {
    expect(formatPhone('1133334444')).toBe('(11) 3333-4444')
  })

  it('formats an 11-digit mobile number', () => {
    expect(formatPhone('11987654321')).toBe('(11) 98765-4321')
  })

  it('truncates input longer than 11 digits', () => {
    expect(formatPhone('119876543219999')).toBe('(11) 98765-4321')
  })
})

describe('formatCep', () => {
  it('formats an 8-digit CEP', () => {
    expect(formatCep('01310100')).toBe('01310-100')
  })

  it('truncates input longer than 8 digits', () => {
    expect(formatCep('013101009999')).toBe('01310-100')
  })
})
