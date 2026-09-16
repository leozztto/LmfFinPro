import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'e-mail é obrigatório').email('e-mail inválido'),
  password: z.string().min(1, 'senha é obrigatória'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  name: z.string().min(1, 'nome é obrigatório'),
  email: z.string().min(1, 'e-mail é obrigatório').email('e-mail inválido'),
  password: z.string().min(8, 'senha deve ter ao menos 8 caracteres'),
  taxRegime: z.string().optional(),
})

export type RegisterFormValues = z.infer<typeof registerSchema>
