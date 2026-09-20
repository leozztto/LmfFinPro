import { z } from 'zod'

export const categoryRuleSchema = z.object({
  pattern: z.string().min(1, 'padrão é obrigatório'),
  categoryId: z.coerce.number({ invalid_type_error: 'selecione uma categoria' }).positive('selecione uma categoria'),
})

export type CategoryRuleFormValues = z.infer<typeof categoryRuleSchema>
