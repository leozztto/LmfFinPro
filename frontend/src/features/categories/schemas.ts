import { z } from 'zod'

export const categorySchema = z.object({
  name: z.string().min(1, 'nome é obrigatório'),
  type: z.enum(['INCOME', 'EXPENSE']),
  color: z.string().optional(),
  icon: z.string().optional(),
})

export type CategoryFormValues = z.infer<typeof categorySchema>
