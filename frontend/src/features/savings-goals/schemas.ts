import { z } from 'zod'

const optionalPercent = z.preprocess(
  (value) => (value === '' || value === null || value === undefined ? undefined : value),
  z.coerce
    .number({ invalid_type_error: 'informe um percentual válido' })
    .min(0, 'o percentual não pode ser negativo')
    .max(100, 'o percentual deve ser no máximo 100')
    .optional(),
)

export const savingsGoalSchema = z.object({
  name: z.string().trim().min(1, 'nome é obrigatório').max(100, 'nome deve ter no máximo 100 caracteres'),
  type: z.enum(['EMERGENCY_FUND', 'TAX_RESERVE', 'VACATION', 'OTHER']),
  targetAmount: z.coerce
    .number({ invalid_type_error: 'informe o valor-alvo' })
    .positive('informe um valor maior que zero'),
  deadline: z.string().optional(),
  /** Percentual (0 a 100) na tela; o backend recebe a fração. */
  incomePercent: optionalPercent,
})

export type SavingsGoalFormValues = z.infer<typeof savingsGoalSchema>

export const contributionSchema = z.object({
  type: z.enum(['DEPOSIT', 'WITHDRAWAL']),
  amount: z.coerce.number({ invalid_type_error: 'informe o valor' }).positive('informe um valor maior que zero'),
  contributionDate: z.string().min(1, 'data é obrigatória'),
  note: z.string().max(255, 'observação deve ter no máximo 255 caracteres').optional(),
})

export type ContributionFormValues = z.infer<typeof contributionSchema>
