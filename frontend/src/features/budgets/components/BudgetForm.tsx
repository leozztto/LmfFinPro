import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useCreateBudget } from '../hooks/useCreateBudget'
import { budgetSchema, type BudgetFormValues } from '../schemas'

interface BudgetFormProps {
  onSuccess?: () => void
}

const EMPTY_VALUES: Partial<BudgetFormValues> = {
  referenceMonth: getCurrentYearMonth(),
  categoryId: undefined,
  limitValue: undefined,
}

export function BudgetForm({ onSuccess }: BudgetFormProps) {
  const createBudget = useCreateBudget()
  const { data: categories } = useCategories()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BudgetFormValues>({
    resolver: zodResolver(budgetSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: EMPTY_VALUES,
  })

  const expenseCategories = (categories ?? []).filter((category) => category.type === 'EXPENSE')

  async function onSubmit(values: BudgetFormValues) {
    try {
      await createBudget.mutateAsync(values)
      showToast('Orçamento criado com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar o orçamento.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Categoria" htmlFor="budget-category" error={errors.categoryId?.message}>
          <Select id="budget-category" defaultValue="" {...register('categoryId')}>
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
        <FormField label="Mês de referência" htmlFor="budget-month" error={errors.referenceMonth?.message}>
          <Input id="budget-month" type="month" {...register('referenceMonth')} />
        </FormField>
        <FormField label="Valor limite" htmlFor="budget-limit" error={errors.limitValue?.message}>
          <Input id="budget-limit" type="number" step="0.01" {...register('limitValue')} />
        </FormField>
      </div>

      <Button type="submit" disabled={createBudget.isPending} className="w-full">
        {createBudget.isPending ? 'Salvando...' : 'Salvar orçamento'}
      </Button>
    </form>
  )
}
