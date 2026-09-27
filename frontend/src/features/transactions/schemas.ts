import { z } from 'zod'

const optionalId = z.preprocess(
  (value) => (value === '' || value === undefined ? undefined : Number(value)),
  z.number().positive().optional(),
)

const blankToUndefined = (value: unknown) => (value === '' || value === undefined || value === null ? undefined : value)

export const transactionSchema = z
  .object({
    accountId: z.coerce.number({ invalid_type_error: 'selecione uma conta' }).positive('selecione uma conta'),
    categoryId: optionalId,
    clientId: optionalId,
    description: z.string().min(1, 'descrição é obrigatória'),
    amount: z.coerce
      .number({ invalid_type_error: 'informe um valor' })
      .positive('informe um valor maior que zero'),
    transactionDate: z.string().min(1, 'data é obrigatória'),
    type: z.enum(['INCOME', 'EXPENSE']),
    // '' = automática: o backend decide pela data (futura = pendente).
    status: z.preprocess(blankToUndefined, z.enum(['PAID', 'PENDING']).optional()),
    // '' = na própria moeda da conta.
    originalCurrency: z.preprocess(blankToUndefined, z.enum(['BRL', 'USD', 'EUR']).optional()),
    originalAmount: z.preprocess(
      blankToUndefined,
      z.coerce
        .number({ invalid_type_error: 'informe o valor' })
        .positive('informe um valor maior que zero')
        .optional(),
    ),
  })
  .superRefine((values, context) => {
    if (values.originalCurrency && values.originalAmount === undefined) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['originalAmount'],
        message: 'informe o valor na moeda da operação',
      })
    }
  })

export type TransactionFormValues = z.infer<typeof transactionSchema>
