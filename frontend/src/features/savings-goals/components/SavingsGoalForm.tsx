import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Checkbox, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCreateSavingsGoal, useSuggestedTaxRate, useUpdateSavingsGoal } from '../hooks/useSavingsGoals'
import { savingsGoalSchema, type SavingsGoalFormValues } from '../schemas'
import type { SavingsGoal, SavingsGoalInput } from '../types'
import { GOAL_TYPE_LABELS, formatPercent, percentToRate, rateToPercent } from '../utils'

interface SavingsGoalFormProps {
  /** Com uma meta, o formulário edita; sem, cria. */
  goal?: SavingsGoal
  onSuccess?: () => void
}

function toFormValues(goal?: SavingsGoal): Partial<SavingsGoalFormValues> {
  if (!goal) return { name: '', type: 'EMERGENCY_FUND', deadline: '', autoContribute: false }
  return {
    name: goal.name,
    type: goal.type,
    targetAmount: goal.targetAmount,
    deadline: goal.deadline ?? '',
    incomePercent: goal.incomeRate === null ? undefined : rateToPercent(goal.incomeRate),
    autoContribute: goal.autoContribute,
  }
}

export function SavingsGoalForm({ goal, onSuccess }: SavingsGoalFormProps) {
  const createGoal = useCreateSavingsGoal()
  const updateGoal = useUpdateSavingsGoal()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<SavingsGoalFormValues>({
    resolver: zodResolver(savingsGoalSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: toFormValues(goal),
  })

  const isTaxReserve = watch('type') === 'TAX_RESERVE'
  const suggestedTaxRate = useSuggestedTaxRate(isTaxReserve)
  const suggestedRate = suggestedTaxRate.data?.incomeRate

  // Na caixinha do imposto, o percentual vazio já vem preenchido com a alíquota sugerida pro regime.
  useEffect(() => {
    const current = getValues('incomePercent')
    if (isTaxReserve && suggestedRate !== undefined && (current === undefined || String(current) === '')) {
      setValue('incomePercent', rateToPercent(suggestedRate))
    }
  }, [isTaxReserve, suggestedRate, getValues, setValue])

  const isPending = createGoal.isPending || updateGoal.isPending

  async function onSubmit(values: SavingsGoalFormValues) {
    const input: SavingsGoalInput = {
      name: values.name,
      type: values.type,
      targetAmount: values.targetAmount,
      deadline: values.deadline ? values.deadline : null,
      incomeRate: values.incomePercent === undefined ? null : percentToRate(values.incomePercent),
      autoContribute: values.autoContribute,
    }
    try {
      if (goal) {
        await updateGoal.mutateAsync({ id: goal.id, input })
        showToast('Meta atualizada com sucesso.', 'success')
      } else {
        await createGoal.mutateAsync(input)
        showToast('Meta criada com sucesso.', 'success')
      }
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível salvar a meta.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Tipo" htmlFor="goal-type" error={errors.type?.message}>
          <Select id="goal-type" {...register('type')}>
            {Object.entries(GOAL_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Nome" htmlFor="goal-name" error={errors.name?.message}>
          <Input id="goal-name" placeholder="Ex.: Viagem de fim de ano" {...register('name')} />
        </FormField>
        <FormField label="Valor-alvo" htmlFor="goal-target" error={errors.targetAmount?.message}>
          <Input id="goal-target" type="number" step="0.01" inputMode="decimal" {...register('targetAmount')} />
        </FormField>
        <FormField label="Prazo (opcional)" htmlFor="goal-deadline" error={errors.deadline?.message}>
          <Input id="goal-deadline" type="date" {...register('deadline')} />
        </FormField>
        <div className="sm:col-span-2">
          <FormField
            label="Separar das receitas, em % (opcional)"
            htmlFor="goal-percent"
            error={errors.incomePercent?.message}
          >
            <Input id="goal-percent" type="number" step="0.01" inputMode="decimal" {...register('incomePercent')} />
          </FormField>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            Com um percentual, a meta sugere quanto separar de cada receita recebida no mês.
            {isTaxReserve &&
              suggestedRate !== undefined &&
              ` Alíquota de referência para o seu regime: ${formatPercent(suggestedRate)}.`}
          </p>
        </div>
        <label
          htmlFor="goal-auto-contribute"
          className="flex items-start gap-2 text-sm text-zinc-700 dark:text-zinc-200 sm:col-span-2"
        >
          <Checkbox id="goal-auto-contribute" className="mt-0.5" {...register('autoContribute')} />
          <span>
            Aporte automático
            <span className="block text-xs text-zinc-500 dark:text-zinc-400">
              Separa a sugestão sozinho, uma vez por dia, sem precisar clicar em "Separar". Exige o percentual acima
              definido.
            </span>
          </span>
        </label>
      </div>

      <Button type="submit" disabled={isPending} className="w-full">
        {isPending ? 'Salvando...' : goal ? 'Salvar alterações' : 'Criar meta'}
      </Button>
    </form>
  )
}
