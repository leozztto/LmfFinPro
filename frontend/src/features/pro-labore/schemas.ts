import { z } from 'zod'

const percent = (label: string) =>
  z.coerce
    .number({ invalid_type_error: `informe ${label}` })
    .min(0, `${label} não pode ser negativo`)
    .max(100, `${label} deve ser no máximo 100%`)

/** Percentuais em 0–100 na tela; o backend recebe a fração. */
export const proLaboreSettingsSchema = z
  .object({
    calculationBase: z.enum(['MONTH_INCOME', 'CURRENT_BALANCE']),
    reservePercent: percent('o percentual de reserva'),
    cashCushionMonths: z.coerce
      .number({ invalid_type_error: 'informe os meses de colchão' })
      .int('use um número inteiro de meses')
      .min(0, 'de 0 a 12 meses')
      .max(12, 'de 0 a 12 meses'),
    taxMode: z.enum(['AUTOMATIC', 'MANUAL']),
    manualTaxPercent: z.string(),
    fixedAmount: z.string(),
    withholdingMode: z.enum(['AUTOMATIC', 'ENABLED', 'DISABLED']),
    /** Vazio = INSS patronal automático pelo regime. */
    employerInssPercent: z.string(),
  })
  .superRefine((values, ctx) => {
    if (values.taxMode === 'MANUAL') {
      const parsed = Number(values.manualTaxPercent)
      if (values.manualTaxPercent.trim() === '' || Number.isNaN(parsed) || parsed < 0 || parsed > 100) {
        ctx.addIssue({ code: 'custom', path: ['manualTaxPercent'], message: 'informe uma alíquota de 0 a 100%' })
      }
    }
    if (values.employerInssPercent.trim() !== '') {
      const parsed = Number(values.employerInssPercent)
      if (Number.isNaN(parsed) || parsed < 0 || parsed > 100) {
        ctx.addIssue({
          code: 'custom',
          path: ['employerInssPercent'],
          message: 'informe de 0 a 100% ou deixe em branco',
        })
      }
    }
    if (values.fixedAmount.trim() !== '') {
      const parsed = Number(values.fixedAmount)
      if (Number.isNaN(parsed) || parsed < 0) {
        ctx.addIssue({ code: 'custom', path: ['fixedAmount'], message: 'informe um valor positivo ou deixe em branco' })
      }
    }
  })

export type ProLaboreSettingsFormValues = z.infer<typeof proLaboreSettingsSchema>
