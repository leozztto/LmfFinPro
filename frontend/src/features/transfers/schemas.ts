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
    // Só entre contas de moedas diferentes; a exigência fica no formulário, que conhece as moedas.
    receivedAmount: z.preprocess(
      (value) => (value === '' || value === undefined || value === null ? undefined : value),
      z.coerce.number({ invalid_type_error: 'informe o valor recebido' }).positive('informe um valor maior que zero').optional(),
    ),
  })
  .refine((values) => values.fromAccountId !== values.toAccountId, {
    message: 'a conta de origem e destino devem ser diferentes',
    path: ['toAccountId'],
  })

export type TransferFormValues = z.infer<typeof transferSchema>
