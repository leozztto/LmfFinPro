import { z } from 'zod'

export const budgetSchema = z.object({
  categoryId: z.coerce.number({ invalid_type_error: 'selecione uma categoria' }).positive('selecione uma categoria'),
  referenceMonth: z.string().min(1, 'mês é obrigatório'),
  limitValue: z.coerce
    .number({ invalid_type_error: 'informe o valor limite' })
    .positive('informe um valor maior que zero'),
})

export type BudgetFormValues = z.infer<typeof budgetSchema>

const optionalMonth = z.preprocess((value) => (value === '' ? undefined : value), z.string().optional())

export const recurringBudgetSchema = z
  .object({
    categoryId: z.coerce.number({ invalid_type_error: 'selecione uma categoria' }).positive('selecione uma categoria'),
    limitValue: z.coerce
      .number({ invalid_type_error: 'informe o valor limite' })
      .positive('informe um valor maior que zero'),
    startMonth: z.string().min(1, 'mês inicial é obrigatório'),
    endMonth: optionalMonth,
  })
  .refine((values) => !values.endMonth || values.endMonth >= values.startMonth, {
    message: 'mês final não pode ser anterior ao inicial',
    path: ['endMonth'],
  })

export type RecurringBudgetFormValues = z.infer<typeof recurringBudgetSchema>

const recurringBudgetBatchItemSchema = z.object({
  categoryId: z.coerce.number({ invalid_type_error: 'selecione uma categoria' }).positive('selecione uma categoria'),
  limitValue: z.coerce
    .number({ invalid_type_error: 'informe o valor limite' })
    .positive('informe um valor maior que zero'),
})

export const recurringBudgetBatchSchema = z
  .object({
    startMonth: z.string().min(1, 'mês inicial é obrigatório'),
    endMonth: optionalMonth,
    items: z
      .array(recurringBudgetBatchItemSchema)
      .min(1, 'adicione ao menos uma categoria')
      .refine((items) => new Set(items.map((item) => item.categoryId)).size === items.length, {
        message: 'cada categoria só pode aparecer uma vez',
      }),
  })
  .refine((values) => !values.endMonth || values.endMonth >= values.startMonth, {
    message: 'mês final não pode ser anterior ao inicial',
    path: ['endMonth'],
  })

export type RecurringBudgetBatchFormValues = z.infer<typeof recurringBudgetBatchSchema>

const recurringBudgetUpdateBaseSchema = z.object({
  limitValue: z.coerce
    .number({ invalid_type_error: 'informe o valor limite' })
    .positive('informe um valor maior que zero'),
  endMonth: optionalMonth,
  active: z.boolean(),
})

export type RecurringBudgetUpdateFormValues = z.infer<typeof recurringBudgetUpdateBaseSchema>

/**
 * `startMonth` não é um campo do formulário de edição (é fixo, vem da recorrência), mas ainda
 * define o piso válido para `endMonth` — por isso o schema é montado com ele em mãos.
 */
export function recurringBudgetUpdateSchema(startMonth: string) {
  return recurringBudgetUpdateBaseSchema.refine(
    (values) => !values.endMonth || values.endMonth >= startMonth,
    { message: 'mês final não pode ser anterior ao inicial', path: ['endMonth'] },
  )
}
