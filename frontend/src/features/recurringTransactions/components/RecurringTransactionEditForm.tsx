import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Checkbox, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { TRANSACTION_TYPE_LABELS } from '@/features/transactions/types'
import { useUpdateRecurringTransaction } from '../hooks/useUpdateRecurringTransaction'
import { recurringTransactionUpdateSchema, type RecurringTransactionUpdateFormValues } from '../schemas'
import { RECURRENCE_FREQUENCY_LABELS, type RecurringTransaction } from '../types'

interface RecurringTransactionEditFormProps {
  recurrence: RecurringTransaction
  onSuccess?: () => void
}

/** Conta, tipo, frequência e primeira ocorrência são fixos depois de criada a recorrência. */
export function RecurringTransactionEditForm({ recurrence, onSuccess }: RecurringTransactionEditFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const updateRecurringTransaction = useUpdateRecurringTransaction()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RecurringTransactionUpdateFormValues>({
    resolver: zodResolver(recurringTransactionUpdateSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      categoryId: recurrence.categoryId ?? undefined,
      clientId: recurrence.clientId ?? undefined,
      description: recurrence.description,
      amount: recurrence.amount,
      endDate: recurrence.endDate ?? undefined,
      active: recurrence.active,
    },
  })

  const accountName = accounts?.find((account) => account.id === recurrence.accountId)?.name ?? '—'
  const categoriesOfType = categories?.filter((category) => category.type === recurrence.type) ?? []

  async function onSubmit(values: RecurringTransactionUpdateFormValues) {
    try {
      await updateRecurringTransaction.mutateAsync({ id: recurrence.id, input: values })
      showToast('Recorrência atualizada com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar a recorrência.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-lg bg-zinc-100 p-3 text-sm dark:bg-zinc-800 sm:col-span-2 sm:grid-cols-4">
        <div>
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">Tipo</dt>
          <dd className="text-zinc-800 dark:text-zinc-100">{TRANSACTION_TYPE_LABELS[recurrence.type]}</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">Frequência</dt>
          <dd className="text-zinc-800 dark:text-zinc-100">{RECURRENCE_FREQUENCY_LABELS[recurrence.frequency]}</dd>
        </div>
        <div className="min-w-0">
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">Conta</dt>
          <dd className="truncate text-zinc-800 dark:text-zinc-100">{accountName}</dd>
        </div>
        <div>
          <dt className="text-xs text-zinc-500 dark:text-zinc-400">Desde</dt>
          <dd className="text-zinc-800 dark:text-zinc-100">{formatDateOnlyBr(recurrence.startDate)}</dd>
        </div>
      </dl>
      <div className="sm:col-span-2">
        <FormField label="Descrição" htmlFor="recurring-edit-description" error={errors.description?.message}>
          <Input id="recurring-edit-description" {...register('description')} />
        </FormField>
      </div>
      <FormField label="Valor" htmlFor="recurring-edit-amount" error={errors.amount?.message}>
        <Input id="recurring-edit-amount" type="number" step="0.01" inputMode="decimal" {...register('amount')} />
      </FormField>
      <FormField label="Termina em (opcional)" htmlFor="recurring-edit-end-date" error={errors.endDate?.message}>
        <Input id="recurring-edit-end-date" type="date" min={recurrence.startDate} {...register('endDate')} />
      </FormField>
      <FormField label="Categoria (opcional)" htmlFor="recurring-edit-category" error={errors.categoryId?.message}>
        <Select id="recurring-edit-category" {...register('categoryId')}>
          <option value="">Sem categoria</option>
          {categoriesOfType.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Cliente (opcional)" htmlFor="recurring-edit-client" error={errors.clientId?.message}>
        <Select id="recurring-edit-client" {...register('clientId')}>
          <option value="">Sem cliente</option>
          {clients?.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
            </option>
          ))}
        </Select>
      </FormField>
      <label htmlFor="recurring-edit-active" className="flex items-start gap-2 text-sm text-zinc-700 dark:text-zinc-200 sm:col-span-2">
        <Checkbox id="recurring-edit-active" className="mt-0.5" {...register('active')} />
        <span>
          Ativa
          <span className="block text-xs text-zinc-500 dark:text-zinc-400">
            Pausada, nenhuma ocorrência é lançada. Ao reativar, as datas que caíram durante a pausa são puladas.
          </span>
        </span>
      </label>
      <p className="text-xs text-zinc-500 dark:text-zinc-400 sm:col-span-2">
        As alterações valem para as próximas ocorrências; transações já lançadas não mudam.
      </p>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={updateRecurringTransaction.isPending} className="w-full">
          {updateRecurringTransaction.isPending ? 'Salvando...' : 'Salvar alterações'}
        </Button>
      </div>
    </form>
  )
}
