import { z } from 'zod'

const optionalId = z.preprocess(
  (value) => (value === '' || value === undefined ? undefined : Number(value)),
  z.number().positive().optional(),
)

const optionalDate = z.preprocess((value) => (value === '' ? undefined : value), z.string().optional())

const amount = z.coerce.number({ invalid_type_error: 'informe um valor' }).positive('informe um valor maior que zero')

export const recurringTransactionSchema = z
  .object({
    accountId: z.coerce.number({ invalid_type_error: 'selecione uma conta' }).positive('selecione uma conta'),
    categoryId: optionalId,
    clientId: optionalId,
    description: z.string().min(1, 'descrição é obrigatória'),
    amount,
    type: z.enum(['INCOME', 'EXPENSE']),
    frequency: z.enum(['WEEKLY', 'MONTHLY', 'YEARLY']),
    startDate: z.string().min(1, 'data inicial é obrigatória'),
    endDate: optionalDate,
  })
  .refine((values) => !values.endDate || values.endDate >= values.startDate, {
    message: 'data final não pode ser anterior à inicial',
    path: ['endDate'],
  })

export type RecurringTransactionFormValues = z.infer<typeof recurringTransactionSchema>

export const recurringTransactionUpdateSchema = z.object({
  categoryId: optionalId,
  clientId: optionalId,
  description: z.string().min(1, 'descrição é obrigatória'),
  amount,
  endDate: optionalDate,
  active: z.boolean(),
})

export type RecurringTransactionUpdateFormValues = z.infer<typeof recurringTransactionUpdateSchema>
