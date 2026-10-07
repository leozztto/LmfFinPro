import { z } from 'zod'

export const householdSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'nome é obrigatório')
    .max(150, 'nome deve ter no máximo 150 caracteres'),
})

export const inviteSchema = z.object({
  email: z.string().trim().min(1, 'e-mail é obrigatório').email('e-mail inválido'),
})

export type HouseholdFormValues = z.infer<typeof householdSchema>
export type InviteFormValues = z.infer<typeof inviteSchema>
