import { z } from 'zod'
import { isValidCpf } from '@/shared/validation/cpf'
import { isValidCnpj } from '@/shared/validation/cnpj'
import { onlyDigits } from '@/shared/format/mask'
import type { DocumentType } from '@/shared/auth/types'

export const loginSchema = z.object({
  email: z.string().min(1, 'e-mail é obrigatório').email('e-mail inválido'),
  password: z.string().min(1, 'senha é obrigatória'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const TAX_REGIME_OPTIONS = [
  { value: 'AUTONOMO', label: 'Autônomo / pessoa física' },
  { value: 'MEI', label: 'MEI' },
  { value: 'SIMPLES_NACIONAL', label: 'Simples Nacional' },
  { value: 'LUCRO_PRESUMIDO', label: 'Lucro presumido' },
  { value: 'OUTRO', label: 'Outro' },
] as const

/** MEI, Simples Nacional e Lucro Presumido são regimes de pessoa jurídica — exigem CNPJ.
 *  Autônomo é pessoa física — exige CPF. "Outro" fica com CPF por padrão (caso mais comum). */
const CNPJ_TAX_REGIMES = new Set(['MEI', 'SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO'])

export function documentTypeForTaxRegime(taxRegime: string): DocumentType {
  return CNPJ_TAX_REGIMES.has(taxRegime) ? 'CNPJ' : 'CPF'
}

const addressSchema = z.object({
  zipCode: z
    .string()
    .min(1, 'CEP é obrigatório')
    .transform(onlyDigits)
    .refine((value) => /^\d{8}$/.test(value), 'CEP deve ter 8 dígitos'),
  street: z.string().min(1, 'logradouro é obrigatório'),
  number: z.string().min(1, 'número é obrigatório'),
  complement: z.string().optional(),
  neighborhood: z.string().min(1, 'bairro é obrigatório'),
  city: z.string().min(1, 'cidade é obrigatória'),
  state: z.string().min(1, 'selecione o estado'),
})

export const registerSchema = z
  .object({
    name: z
      .string()
      .min(1, 'nome é obrigatório')
      .regex(/^\S+\s+\S+/, 'informe nome e sobrenome'),
    email: z.string().min(1, 'e-mail é obrigatório').email('e-mail inválido'),
    taxRegime: z.string().min(1, 'selecione um regime tributário'),
    documentNumber: z.string().min(1, 'documento é obrigatório'),
    phone: z
      .string()
      .optional()
      .transform((value) => (value ? onlyDigits(value) : undefined))
      .refine((value) => !value || /^\d{10,11}$/.test(value), 'telefone deve ter DDD + número (10 ou 11 dígitos)'),
    address: addressSchema,
    password: z.string().min(8, 'senha deve ter ao menos 8 caracteres'),
    confirmPassword: z.string().min(1, 'confirme a senha'),
    acceptedTerms: z.boolean().refine((value) => value, 'você precisa aceitar os termos de uso'),
  })
  .superRefine((data, ctx) => {
    const documentType = documentTypeForTaxRegime(data.taxRegime)
    const digits = onlyDigits(data.documentNumber)

    if (documentType === 'CPF' && !(digits.length === 11 && isValidCpf(digits))) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['documentNumber'], message: 'CPF inválido' })
    }
    if (documentType === 'CNPJ' && !(digits.length === 14 && isValidCnpj(digits))) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['documentNumber'], message: 'CNPJ inválido' })
    }
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'as senhas não coincidem',
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
