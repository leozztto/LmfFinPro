import { describe, expect, it } from 'vitest'
import { formatCurrency } from './currency'

/** O Intl separa o símbolo do valor com espaço inabalável. */
const NBSP = ' '

describe('formatCurrency', () => {
  it('formats positive values as BRL currency', () => {
    expect(formatCurrency(1234.5)).toBe(`R$${NBSP}1.234,50`)
  })

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe(`R$${NBSP}0,00`)
  })

  it('formats negative values with a leading minus sign', () => {
    expect(formatCurrency(-50)).toBe(`-R$${NBSP}50,00`)
  })

  it('formats other currencies in the Brazilian style', () => {
    expect(formatCurrency(1234.5, 'USD')).toBe(`US$${NBSP}1.234,50`)
    expect(formatCurrency(20, 'EUR')).toBe(`€${NBSP}20,00`)
  })
})
