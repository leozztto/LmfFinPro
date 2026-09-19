import { z } from 'zod'

const optionalId = z.preprocess(
  (value) => (value === '' || value === undefined ? undefined : Number(value)),
  z.number().positive().optional(),
)

export const transactionSchema = z.object({
  accountId: z.coerce.number({ invalid_type_error: 'selecione uma conta' }).positive('selecione uma conta'),
  categoryId: optionalId,
  clientId: optionalId,
  description: z.string().min(1, 'descrição é obrigatória'),
  amount: z.coerce
    .number({ invalid_type_error: 'informe um valor' })
    .positive('informe um valor maior que zero'),
  transactionDate: z.string().min(1, 'data é obrigatória'),
  type: z.enum(['INCOME', 'EXPENSE']),
})

export type TransactionFormValues = z.infer<typeof transactionSchema>
