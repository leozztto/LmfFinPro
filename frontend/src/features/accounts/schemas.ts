import { z } from 'zod'

export const accountSchema = z.object({
  name: z.string().min(1, 'nome é obrigatório'),
  type: z.enum(['CHECKING', 'SAVINGS', 'WALLET', 'INVESTMENT']),
  initialBalance: z.coerce.number({ invalid_type_error: 'informe um valor numérico' }),
  scope: z.enum(['PERSONAL', 'BUSINESS']).default('PERSONAL'),
})

export type AccountFormValues = z.infer<typeof accountSchema>

export const valuationSchema = z.object({
  valuationDate: z.string().min(1, 'data é obrigatória'),
  value: z.coerce
    .number({ invalid_type_error: 'informe o valor' })
    .min(0, 'o valor não pode ser negativo'),
})

export type ValuationFormValues = z.infer<typeof valuationSchema>
