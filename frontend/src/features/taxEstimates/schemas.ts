import { z } from 'zod'

export const taxEstimateSchema = z.object({
  referenceMonth: z.string().min(1, 'mês é obrigatório'),
  regime: z.enum(['AUTONOMO', 'MEI', 'SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'OUTRO'], {
    errorMap: () => ({ message: 'selecione um regime' }),
  }),
  grossRevenue: z.coerce
    .number({ invalid_type_error: 'informe a receita bruta' })
    .positive('informe uma receita maior que zero'),
  appliedRate: z.coerce
    .number({ invalid_type_error: 'informe a alíquota' })
    .min(0, 'alíquota não pode ser negativa'),
})

export type TaxEstimateFormValues = z.infer<typeof taxEstimateSchema>
