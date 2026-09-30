import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatCurrency } from '@/shared/format/currency'
import { getCurrentIsoDate } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useAddContribution } from '../hooks/useSavingsGoals'
import { contributionSchema, type ContributionFormValues } from '../schemas'
import type { SavingsGoal } from '../types'

interface ContributionFormProps {
  goal: SavingsGoal
  onSuccess?: () => void
}

export function ContributionForm({ goal, onSuccess }: ContributionFormProps) {
  const { data: accounts } = useAccounts()
  const addContribution = useAddContribution()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ContributionFormValues>({
    resolver: zodResolver(contributionSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { type: 'DEPOSIT', contributionDate: getCurrentIsoDate(), note: '' },
  })

  const isWithdrawal = watch('type') === 'WITHDRAWAL'
  const accountName = (accountId: number) => accounts?.find((account) => account.id === accountId)?.name ?? '—'

  async function onSubmit(values: ContributionFormValues) {
    try {
      await addContribution.mutateAsync({
        goalId: goal.id,
        input: {
          type: values.type,
          amount: values.amount,
          contributionDate: values.contributionDate,
          note: values.note?.trim() ? values.note.trim() : null,
        },
      })
      showToast(values.type === 'DEPOSIT' ? 'Aporte registrado.' : 'Resgate registrado.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível registrar a movimentação.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        {isWithdrawal
          ? `O resgate vira uma transferência de ${accountName(goal.accountId)} para ${accountName(goal.fundingAccountId)}.`
          : `O aporte vira uma transferência de ${accountName(goal.fundingAccountId)} para ${accountName(goal.accountId)}.`}
      </p>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Movimentação" htmlFor="contribution-type" error={errors.type?.message}>
          <Select id="contribution-type" {...register('type')}>
            <option value="DEPOSIT">Aporte (guardar)</option>
            <option value="WITHDRAWAL">Resgate (retirar)</option>
          </Select>
        </FormField>
        <FormField label="Valor" htmlFor="contribution-amount" error={errors.amount?.message}>
          <Input
            id="contribution-amount"
            type="number"
            step="0.01"
            inputMode="decimal"
            {...register('amount')}
          />
        </FormField>
        <FormField label="Data" htmlFor="contribution-date" error={errors.contributionDate?.message}>
          <Input id="contribution-date" type="date" {...register('contributionDate')} />
        </FormField>
        <FormField label="Observação (opcional)" htmlFor="contribution-note" error={errors.note?.message}>
          <Input id="contribution-note" {...register('note')} />
        </FormField>
      </div>
      {isWithdrawal && (
        <p className="text-xs text-zinc-500 dark:text-zinc-400">
          Disponível para resgate: {formatCurrency(goal.savedAmount)}.
        </p>
      )}

      <Button type="submit" disabled={addContribution.isPending} className="w-full">
        {addContribution.isPending ? 'Salvando...' : isWithdrawal ? 'Registrar resgate' : 'Registrar aporte'}
      </Button>
    </form>
  )
}
