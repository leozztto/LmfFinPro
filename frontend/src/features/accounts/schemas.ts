import { z } from 'zod'

export const accountSchema = z.object({
  name: z.string().min(1, 'nome é obrigatório'),
  type: z.enum(['CHECKING', 'SAVINGS', 'WALLET']),
  initialBalance: z.coerce.number({ invalid_type_error: 'informe um valor numérico' }),
})

export type AccountFormValues = z.infer<typeof accountSchema>
