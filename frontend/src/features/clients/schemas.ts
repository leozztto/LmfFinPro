import { z } from 'zod'
import { isValidCpf } from '@/shared/validation/cpf'
import { isValidCnpj } from '@/shared/validation/cnpj'
import { onlyDigits } from '@/shared/format/mask'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const clientSchema = z
  .object({
    name: z.string(),
    email: z.string().optional(),
    phone: z.string().optional(),
    documentType: z.enum(['CPF', 'CNPJ', '']).optional(),
    documentNumber: z.string().optional(),
    workType: z.enum(['PJ', 'AUTONOMO', '']).optional(),
    notes: z.string().optional(),
    color: z.string().optional(),
    active: z.boolean(),
  })
  .superRefine((data, ctx) => {
    const addIssue = (path: (string | number)[], message: string) =>
      ctx.addIssue({ code: z.ZodIssueCode.custom, path, message })

    if (!data.name.trim()) {
      addIssue(['name'], 'nome é obrigatório')
    }

    if (!data.email?.trim()) {
      addIssue(['email'], 'e-mail é obrigatório')
    } else if (!EMAIL_PATTERN.test(data.email)) {
      addIssue(['email'], 'e-mail inválido')
    }

    const phoneDigits = data.phone ? onlyDigits(data.phone) : ''
    if (!phoneDigits) {
      addIssue(['phone'], 'telefone é obrigatório')
    } else if (!/^\d{10,11}$/.test(phoneDigits)) {
      addIssue(['phone'], 'telefone deve ter DDD + número (10 ou 11 dígitos)')
    }

    if (!data.workType) {
      addIssue(['workType'], 'selecione o tipo de trabalho')
    }

    const documentDigits = data.documentNumber ? onlyDigits(data.documentNumber) : ''
    if (!data.documentType) {
      addIssue(['documentType'], 'selecione o tipo de documento')
    }
    if (!documentDigits) {
      addIssue(['documentNumber'], 'documento é obrigatório')
    } else if (data.documentType === 'CPF' && !(documentDigits.length === 11 && isValidCpf(documentDigits))) {
      addIssue(['documentNumber'], 'CPF inválido')
    } else if (data.documentType === 'CNPJ' && !(documentDigits.length === 14 && isValidCnpj(documentDigits))) {
      addIssue(['documentNumber'], 'CNPJ inválido')
    }

    if ((data.notes?.length ?? 0) > 1000) {
      addIssue(['notes'], 'observações devem ter no máximo 1000 caracteres')
    }
  })

export type ClientFormValues = z.infer<typeof clientSchema>
