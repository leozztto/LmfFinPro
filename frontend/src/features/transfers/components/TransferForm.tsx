import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCreateTransfer } from '../hooks/useCreateTransfer'
import { transferSchema, type TransferFormValues } from '../schemas'
import { getCurrentIsoDate } from '@/shared/format/date'

interface TransferFormProps {
  onSuccess?: () => void
}

export function TransferForm({ onSuccess }: TransferFormProps) {
  const { data: accounts } = useAccounts()
  const createTransfer = useCreateTransfer()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TransferFormValues>({
    resolver: zodResolver(transferSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      transferDate: getCurrentIsoDate(),
    },
  })

  async function onSubmit(values: TransferFormValues) {
    try {
      await createTransfer.mutateAsync({
        fromAccountId: values.fromAccountId,
        toAccountId: values.toAccountId,
        amount: values.amount,
        transferDate: values.transferDate,
        description: values.description || undefined,
      })
      reset({
        amount: 0,
        description: '',
        transferDate: getCurrentIsoDate(),
      })
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível transferir.')
    }
  }

  if (!accounts || accounts.length < 2) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Cadastre ao menos duas contas para transferir valores entre elas.
      </p>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Conta de origem" htmlFor="transfer-from-account" error={errors.fromAccountId?.message}>
        <Select id="transfer-from-account" defaultValue="" {...register('fromAccountId')}>
          <option value="" disabled>
            Selecione...
          </option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Conta de destino" htmlFor="transfer-to-account" error={errors.toAccountId?.message}>
        <Select id="transfer-to-account" defaultValue="" {...register('toAccountId')}>
          <option value="" disabled>
            Selecione...
          </option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Valor" htmlFor="transfer-amount" error={errors.amount?.message}>
        <Input id="transfer-amount" type="number" step="0.01" {...register('amount')} />
      </FormField>
      <FormField label="Data" htmlFor="transfer-date" error={errors.transferDate?.message}>
        <Input id="transfer-date" type="date" {...register('transferDate')} />
      </FormField>
      <div className="sm:col-span-2">
        <FormField label="Descrição (opcional)" htmlFor="transfer-description" error={errors.description?.message}>
          <Input id="transfer-description" placeholder="Ex: reserva de emergência" {...register('description')} />
        </FormField>
      </div>
      <div className="sm:col-span-2 space-y-3">
        <Button type="submit" disabled={createTransfer.isPending} className="w-full">
          {createTransfer.isPending ? 'Transferindo...' : 'Transferir'}
        </Button>
      </div>
    </form>
  )
}
