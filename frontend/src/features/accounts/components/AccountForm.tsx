import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatCurrency } from '@/shared/format/currency'
import { useCreateAccount } from '../hooks/useCreateAccount'
import { useUpdateAccount } from '../hooks/useUpdateAccount'
import { accountSchema, type AccountFormValues } from '../schemas'
import { ACCOUNT_TYPE_LABELS, type Account } from '../types'

interface AccountFormProps {
  account?: Account
  onSuccess?: () => void
}

export function AccountForm({ account, onSuccess }: AccountFormProps) {
  const isEditing = account != null
  const createAccount = useCreateAccount()
  const updateAccount = useUpdateAccount()
  const isPending = isEditing ? updateAccount.isPending : createAccount.isPending
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
    defaultValues: isEditing
      ? { name: account.name, type: account.type, initialBalance: account.initialBalance }
      : { type: 'CHECKING', initialBalance: 0 },
  })

  async function onSubmit(values: AccountFormValues) {
    try {
      if (isEditing) {
        await updateAccount.mutateAsync({
          id: account.id,
          input: { name: values.name, type: values.type, initialBalance: values.initialBalance },
        })
        showToast('Conta atualizada com sucesso.', 'success')
      } else {
        await createAccount.mutateAsync(values)
        reset({ name: '', type: 'CHECKING', initialBalance: 0 })
        showToast('Conta criada com sucesso.', 'success')
      }
      onSuccess?.()
    } catch (error) {
      showToast(
        error instanceof ApiError ? error.message : `Não foi possível ${isEditing ? 'atualizar' : 'criar'} a conta.`,
      )
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
        <Input id="account-balance" type="number" step="0.01" disabled={isEditing} {...register('initialBalance')} />
      </FormField>
      {isEditing && (
        <FormField label="Saldo atual" htmlFor="account-current-balance">
          <Input id="account-current-balance" value={formatCurrency(account.currentBalance)} disabled readOnly />
        </FormField>
      )}
      <div className="sm:col-span-2">
        <Button type="submit" disabled={isPending} className="w-full">
          {isPending ? 'Salvando...' : isEditing ? 'Salvar alterações' : 'Adicionar conta'}
        </Button>
      </div>
    </form>
  )
}
