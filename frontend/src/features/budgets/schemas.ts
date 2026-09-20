import { z } from 'zod'

export const budgetSchema = z.object({
  categoryId: z.coerce.number({ invalid_type_error: 'selecione uma categoria' }).positive('selecione uma categoria'),
  referenceMonth: z.string().min(1, 'mês é obrigatório'),
  limitValue: z.coerce
    .number({ invalid_type_error: 'informe o valor limite' })
    .positive('informe um valor maior que zero'),
})

export type BudgetFormValues = z.infer<typeof budgetSchema>
