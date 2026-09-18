import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCreateAccount } from '../hooks/useCreateAccount'
import { accountSchema, type AccountFormValues } from '../schemas'
import { ACCOUNT_TYPE_LABELS } from '../types'

interface AccountFormProps {
  onSuccess?: () => void
}

export function AccountForm({ onSuccess }: AccountFormProps) {
  const createAccount = useCreateAccount()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AccountFormValues>({
    resolver: zodResolver(accountSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { type: 'CHECKING', initialBalance: 0 },
  })

  async function onSubmit(values: AccountFormValues) {
    try {
      await createAccount.mutateAsync(values)
      reset({ name: '', type: 'CHECKING', initialBalance: 0 })
      showToast('Conta criada com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar a conta.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Nome" htmlFor="account-name" error={errors.name?.message}>
        <Input id="account-name" placeholder="Conta corrente Nubank" {...register('name')} />
      </FormField>
      <FormField label="Tipo" htmlFor="account-type" error={errors.type?.message}>
        <Select id="account-type" {...register('type')}>
          {Object.entries(ACCOUNT_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Saldo inicial" htmlFor="account-balance" error={errors.initialBalance?.message}>
        <Input id="account-balance" type="number" step="0.01" {...register('initialBalance')} />
      </FormField>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={createAccount.isPending} className="w-full">
          {createAccount.isPending ? 'Salvando...' : 'Adicionar conta'}
        </Button>
      </div>
    </form>
  )
}
