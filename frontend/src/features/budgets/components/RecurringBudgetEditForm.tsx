import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Checkbox, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useUpdateRecurringBudget } from '../hooks/useUpdateRecurringBudget'
import { recurringBudgetUpdateSchema, type RecurringBudgetUpdateFormValues } from '../schemas'
import type { RecurringBudget } from '../types'

interface RecurringBudgetEditFormProps {
  recurrence: RecurringBudget
  onSuccess?: () => void
}

/** Categoria e mês inicial são fixos depois de criada a recorrência. */
export function RecurringBudgetEditForm({ recurrence, onSuccess }: RecurringBudgetEditFormProps) {
  const { data: categories } = useCategories()
  const updateRecurringBudget = useUpdateRecurringBudget()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RecurringBudgetUpdateFormValues>({
    resolver: zodResolver(recurringBudgetUpdateSchema(recurrence.startMonth)),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      limitValue: recurrence.limitValue,
      endMonth: recurrence.endMonth ?? undefined,
      active: recurrence.active,
    },
  })

  const categoryName = categories?.find((category) => category.id === recurrence.categoryId)?.name ?? '—'

  async function onSubmit(values: RecurringBudgetUpdateFormValues) {
    try {
      await updateRecurringBudget.mutateAsync({ id: recurrence.id, input: values })
      showToast('Orçamento recorrente atualizado com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar o orçamento recorrente.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg bg-zinc-100 p-3 text-sm dark:bg-zinc-800 sm:col-span-2">
        <div className="min-w-0">
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">Categoria</dt>
          <dd className="truncate text-zinc-800 dark:text-zinc-100">{categoryName}</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">A partir de</dt>
          <dd className="text-zinc-800 dark:text-zinc-100">{formatMonthLabel(recurrence.startMonth)}</dd>
        </div>
      </dl>
      <FormField label="Valor limite" htmlFor="recurring-budget-edit-limit" error={errors.limitValue?.message}>
        <Input id="recurring-budget-edit-limit" type="number" step="0.01" {...register('limitValue')} />
      </FormField>
      <FormField label="Até (opcional)" htmlFor="recurring-budget-edit-end" error={errors.endMonth?.message}>
        <Input id="recurring-budget-edit-end" type="month" min={recurrence.startMonth} {...register('endMonth')} />
      </FormField>
      <label
        htmlFor="recurring-budget-edit-active"
        className="flex items-start gap-2 text-sm text-zinc-700 dark:text-zinc-200 sm:col-span-2"
      >
        <Checkbox id="recurring-budget-edit-active" className="mt-0.5" {...register('active')} />
        <span>
          Ativo
          <span className="block text-xs text-zinc-500 dark:text-zinc-400">
            Pausado, nenhum orçamento é lançado. Ao reativar, os meses que caíram durante a pausa são pulados.
          </span>
        </span>
      </label>
      <p className="text-xs text-zinc-500 dark:text-zinc-400 sm:col-span-2">
        As alterações valem para os próximos meses; orçamentos já lançados não mudam.
      </p>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={updateRecurringBudget.isPending} className="w-full">
          {updateRecurringBudget.isPending ? 'Salvando...' : 'Salvar alterações'}
        </Button>
      </div>
    </form>
  )
}
