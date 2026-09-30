import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useCreateRecurringBudget } from '../hooks/useCreateRecurringBudget'
import { recurringBudgetSchema, type RecurringBudgetFormValues } from '../schemas'

interface RecurringBudgetFormProps {
  onSuccess?: () => void
}

const EMPTY_VALUES: Partial<RecurringBudgetFormValues> = {
  startMonth: getCurrentYearMonth(),
  categoryId: undefined,
  limitValue: undefined,
  endMonth: undefined,
}

export function RecurringBudgetForm({ onSuccess }: RecurringBudgetFormProps) {
  const createRecurringBudget = useCreateRecurringBudget()
  const { data: categories } = useCategories()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RecurringBudgetFormValues>({
    resolver: zodResolver(recurringBudgetSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: EMPTY_VALUES,
  })

  const expenseCategories = (categories ?? []).filter((category) => category.type === 'EXPENSE')

  async function onSubmit(values: RecurringBudgetFormValues) {
    try {
      await createRecurringBudget.mutateAsync(values)
      showToast('Orçamento recorrente criado com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar o orçamento recorrente.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FormField label="Categoria" htmlFor="recurring-budget-category" error={errors.categoryId?.message}>
          <Select id="recurring-budget-category" defaultValue="" {...register('categoryId')}>
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
        <FormField label="Valor limite" htmlFor="recurring-budget-limit" error={errors.limitValue?.message}>
          <Input id="recurring-budget-limit" type="number" step="0.01" {...register('limitValue')} />
        </FormField>
        <FormField label="A partir de" htmlFor="recurring-budget-start" error={errors.startMonth?.message}>
          <Input id="recurring-budget-start" type="month" {...register('startMonth')} />
        </FormField>
        <FormField label="Até (opcional)" htmlFor="recurring-budget-end" error={errors.endMonth?.message}>
          <Input id="recurring-budget-end" type="month" {...register('endMonth')} />
        </FormField>
      </div>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Um orçamento é lançado automaticamente para essa categoria todo início de mês, até o mês final (ou
        indefinidamente, se não informado).
      </p>

      <Button type="submit" disabled={createRecurringBudget.isPending} className="w-full">
        {createRecurringBudget.isPending ? 'Salvando...' : 'Salvar orçamento recorrente'}
      </Button>
    </form>
  )
}
