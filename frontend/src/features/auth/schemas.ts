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

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Todos os campos do objeto base ficam como z.string()/z.boolean() "crus", sem `.min()`/`.email()`
 * embutidos, e TODA a validação — incluindo a checagem cruzada de CPF/CNPJ conforme o regime
 * tributário e a confirmação de senha — roda dentro de um único `.superRefine()`.
 *
 * Isso não é estético, é necessário: no Zod, `.refine()`/`.superRefine()` encadeados a um
 * `z.object()` só executam se TODOS os campos daquele objeto já tiverem passado a validação da
 * própria cadeia. Se qualquer campo tivesse seu próprio `.min(1, ...)`, um único campo vazio
 * (ex: nome) seria suficiente para o Zod nunca chegar a validar o CPF/CNPJ — que é exatamente o
 * bug que este arquivo corrige: a validação de documento parecia simplesmente não rodar.
 */
export const registerSchema = z
  .object({
    name: z.string(),
    email: z.string(),
    taxRegime: z.string(),
    documentNumber: z.string(),
    phone: z.string().optional(),
    address: z.object({
      zipCode: z.string(),
      street: z.string(),
      number: z.string(),
      complement: z.string().optional(),
      neighborhood: z.string(),
      city: z.string(),
      state: z.string(),
    }),
    password: z.string(),
    confirmPassword: z.string(),
    acceptedTerms: z.boolean(),
  })
  .superRefine((data, ctx) => {
    const addIssue = (path: (string | number)[], message: string) =>
      ctx.addIssue({ code: z.ZodIssueCode.custom, path, message })

    if (!data.name.trim()) {
      addIssue(['name'], 'nome é obrigatório')
    } else if (!/^\S+\s+\S+/.test(data.name)) {
      addIssue(['name'], 'informe nome e sobrenome')
    }

    if (!data.email.trim()) {
      addIssue(['email'], 'e-mail é obrigatório')
    } else if (!EMAIL_PATTERN.test(data.email)) {
      addIssue(['email'], 'e-mail inválido')
    }

    if (!data.taxRegime) {
      addIssue(['taxRegime'], 'selecione um regime tributário')
    }

    const documentType = documentTypeForTaxRegime(data.taxRegime)
    const documentDigits = onlyDigits(data.documentNumber)
    if (!data.documentNumber.trim()) {
      addIssue(['documentNumber'], 'documento é obrigatório')
    } else if (documentType === 'CPF' && !(documentDigits.length === 11 && isValidCpf(documentDigits))) {
      addIssue(['documentNumber'], 'CPF inválido')
    } else if (documentType === 'CNPJ' && !(documentDigits.length === 14 && isValidCnpj(documentDigits))) {
      addIssue(['documentNumber'], 'CNPJ inválido')
    }

    const phoneDigits = data.phone ? onlyDigits(data.phone) : ''
    if (phoneDigits && !/^\d{10,11}$/.test(phoneDigits)) {
      addIssue(['phone'], 'telefone deve ter DDD + número (10 ou 11 dígitos)')
    }

    if (!data.address.zipCode.trim()) {
      addIssue(['address', 'zipCode'], 'CEP é obrigatório')
    } else if (!/^\d{8}$/.test(onlyDigits(data.address.zipCode))) {
      addIssue(['address', 'zipCode'], 'CEP deve ter 8 dígitos')
    }
    if (!data.address.street.trim()) addIssue(['address', 'street'], 'logradouro é obrigatório')
    if (!data.address.number.trim()) addIssue(['address', 'number'], 'número é obrigatório')
    if (!data.address.neighborhood.trim()) addIssue(['address', 'neighborhood'], 'bairro é obrigatório')
    if (!data.address.city.trim()) addIssue(['address', 'city'], 'cidade é obrigatória')
    if (!data.address.state) addIssue(['address', 'state'], 'selecione o estado')

    if (data.password.length < 8) {
      addIssue(['password'], 'senha deve ter ao menos 8 caracteres')
    }
    if (!data.confirmPassword) {
      addIssue(['confirmPassword'], 'confirme a senha')
    } else if (data.password !== data.confirmPassword) {
      addIssue(['confirmPassword'], 'as senhas não coincidem')
    }

    if (!data.acceptedTerms) {
      addIssue(['acceptedTerms'], 'você precisa aceitar os termos de uso')
    }
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
