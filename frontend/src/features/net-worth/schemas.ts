import { z } from 'zod'

const debtFields = {
  name: z.string().trim().min(1, 'nome é obrigatório').max(100, 'nome deve ter no máximo 100 caracteres'),
  type: z.enum(['FINANCING', 'LOAN', 'CREDIT_CARD', 'OTHER']),
  creditor: z.string().trim().max(100, 'credor deve ter no máximo 100 caracteres').optional(),
}

const balanceFields = {
  balance: z.coerce
    .number({ invalid_type_error: 'informe o saldo devedor' })
    .min(0, 'o saldo devedor não pode ser negativo'),
  balanceDate: z.string().min(1, 'data é obrigatória'),
}

/** Na criação a dívida já nasce com o saldo devedor atual. */
export const debtCreateSchema = z.object({ ...debtFields, ...balanceFields })
export type DebtCreateFormValues = z.infer<typeof debtCreateSchema>

export const debtUpdateSchema = z.object(debtFields)
export type DebtUpdateFormValues = z.infer<typeof debtUpdateSchema>

export const debtBalanceSchema = z.object(balanceFields)
export type DebtBalanceFormValues = z.infer<typeof debtBalanceSchema>
