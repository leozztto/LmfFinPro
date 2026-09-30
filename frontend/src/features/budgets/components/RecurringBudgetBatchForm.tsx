import { type DefaultValues, useFieldArray, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, IconButton, Input, Select } from '@/shared/ui'
import { PlusIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useCreateRecurringBudgetBatch } from '../hooks/useCreateRecurringBudgetBatch'
import { recurringBudgetBatchSchema, type RecurringBudgetBatchFormValues } from '../schemas'

interface RecurringBudgetBatchFormProps {
  onSuccess?: () => void
}

type BatchItem = RecurringBudgetBatchFormValues['items'][number]

const EMPTY_ITEM = { categoryId: undefined, limitValue: undefined } as unknown as BatchItem

const EMPTY_VALUES: DefaultValues<RecurringBudgetBatchFormValues> = {
  startMonth: getCurrentYearMonth(),
  endMonth: undefined,
  items: [EMPTY_ITEM],
}

/** Cria um orçamento recorrente por categoria de uma vez, todos com o mesmo período — um "pacote" mensal. */
export function RecurringBudgetBatchForm({ onSuccess }: RecurringBudgetBatchFormProps) {
  const createBatch = useCreateRecurringBudgetBatch()
  const { data: categories } = useCategories()
  const { showToast } = useToast()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RecurringBudgetBatchFormValues>({
    resolver: zodResolver(recurringBudgetBatchSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: EMPTY_VALUES,
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'items' })

  const expenseCategories = (categories ?? []).filter((category) => category.type === 'EXPENSE')
  const itemsError = errors.items?.root?.message

  async function onSubmit(values: RecurringBudgetBatchFormValues) {
    try {
      const created = await createBatch.mutateAsync(values)
      showToast(`${created.length} orçamentos recorrentes criados com sucesso.`, 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar os orçamentos recorrentes.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FormField label="A partir de" htmlFor="recurring-budget-batch-start" error={errors.startMonth?.message}>
          <Input id="recurring-budget-batch-start" type="month" {...register('startMonth')} />
        </FormField>
        <FormField label="Até (opcional)" htmlFor="recurring-budget-batch-end" error={errors.endMonth?.message}>
          <Input id="recurring-budget-batch-end" type="month" {...register('endMonth')} />
        </FormField>
      </div>

      <div className="space-y-2">
        {fields.map((field, index) => (
          <div key={field.id} className="flex items-start gap-2">
            <div className="flex-1">
              <FormField
                label="Categoria"
                htmlFor={`recurring-budget-batch-category-${index}`}
                error={errors.items?.[index]?.categoryId?.message}
              >
                <Select
                  id={`recurring-budget-batch-category-${index}`}
                  defaultValue=""
                  {...register(`items.${index}.categoryId`)}
                >
                  <option value="" disabled>
                    Selecione...
                  </option>
                  {expenseCategories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </Select>
              </FormField>
            </div>
            <div className="w-32 shrink-0">
              <FormField
                label="Valor limite"
                htmlFor={`recurring-budget-batch-limit-${index}`}
                error={errors.items?.[index]?.limitValue?.message}
              >
                <Input
                  id={`recurring-budget-batch-limit-${index}`}
                  type="number"
                  step="0.01"
                  {...register(`items.${index}.limitValue`)}
                />
              </FormField>
            </div>
            <IconButton
              icon={TrashIcon}
              label="Remover categoria"
              onClick={() => remove(index)}
              disabled={fields.length === 1}
              className="mt-6 shrink-0"
            />
          </div>
        ))}
        {itemsError && <p className="text-sm text-red-600 dark:text-red-400">{itemsError}</p>}
        <Button type="button" variant="secondary" onClick={() => append(EMPTY_ITEM)} className="w-full sm:w-auto">
          <PlusIcon className="h-4 w-4" /> Adicionar categoria
        </Button>
      </div>

      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Um orçamento é lançado automaticamente para cada categoria acima todo início de mês, até o mês final (ou
        indefinidamente, se não informado).
      </p>

      <Button type="submit" disabled={createBatch.isPending} className="w-full">
        {createBatch.isPending ? 'Salvando...' : 'Salvar orçamentos recorrentes'}
      </Button>
    </form>
  )
}
