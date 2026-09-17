import { z } from 'zod'

export const transferSchema = z
  .object({
    fromAccountId: z.coerce
      .number({ invalid_type_error: 'selecione a conta de origem' })
      .positive('selecione a conta de origem'),
    toAccountId: z.coerce
      .number({ invalid_type_error: 'selecione a conta de destino' })
      .positive('selecione a conta de destino'),
    amount: z.coerce.number({ invalid_type_error: 'informe um valor' }).positive('informe um valor maior que zero'),
    transferDate: z.string().min(1, 'data é obrigatória'),
    description: z.string().optional(),
  })
  .refine((values) => values.fromAccountId !== values.toAccountId, {
    message: 'a conta de origem e destino devem ser diferentes',
    path: ['toAccountId'],
  })

export type TransferFormValues = z.infer<typeof transferSchema>
