import { describe, expect, it } from 'vitest'
import { proLaboreSettingsSchema } from './schemas'

const VALID = {
  calculationBase: 'MONTH_INCOME',
  reservePercent: '10',
  cashCushionMonths: '1',
  taxMode: 'AUTOMATIC',
  manualTaxPercent: '',
  fixedAmount: '',
  withholdingMode: 'AUTOMATIC',
  employerInssPercent: '',
}

describe('proLaboreSettingsSchema', () => {
  it('accepts the defaults with no manual rate and no fixed amount', () => {
    expect(proLaboreSettingsSchema.safeParse(VALID).success).toBe(true)
  })

  it('requires the manual rate only in manual tax mode', () => {
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, taxMode: 'MANUAL' }).success).toBe(false)
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, taxMode: 'MANUAL', manualTaxPercent: '8' }).success).toBe(true)
  })

  it('rejects out-of-range reserve, cushion and negative fixed amount', () => {
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, reservePercent: '150' }).success).toBe(false)
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, cashCushionMonths: '13' }).success).toBe(false)
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, fixedAmount: '-100' }).success).toBe(false)
  })

  it('accepts a blank employer INSS (automatic) and rejects out-of-range values', () => {
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, employerInssPercent: '20' }).success).toBe(true)
    expect(proLaboreSettingsSchema.safeParse({ ...VALID, employerInssPercent: '120' }).success).toBe(false)
  })
})
