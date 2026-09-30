import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { CURRENCIES, CURRENCY_LABELS, formatCurrency } from '@/shared/format/currency'
import { useCreateAccount } from '../hooks/useCreateAccount'
import { useUpdateAccount } from '../hooks/useUpdateAccount'
import { accountSchema, type AccountFormValues } from '../schemas'
import { ACCOUNT_SCOPE_LABELS, ACCOUNT_TYPE_LABELS, type Account } from '../types'

interface AccountFormProps {
  account?: Account
  onSuccess?: () => void
}

const EMPTY_VALUES: AccountFormValues = {
  name: '',
  type: 'CHECKING',
  initialBalance: 0,
  scope: 'PERSONAL',
  currency: 'BRL',
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
    watch,
    formState: { errors },
  } = useForm<AccountFormValues>({
    resolver: zodResolver(accountSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: isEditing
      ? {
          name: account.name,
          type: account.type,
          initialBalance: account.initialBalance,
          scope: account.scope,
          currency: account.currency,
        }
      : EMPTY_VALUES,
  })
  const currency = watch('currency')
  const currencyLocked = isEditing && account.hasEntries

  async function onSubmit(values: AccountFormValues) {
    try {
      if (isEditing) {
        await updateAccount.mutateAsync({ id: account.id, input: values })
        showToast('Conta atualizada com sucesso.', 'success')
      } else {
        await createAccount.mutateAsync(values)
        reset(EMPTY_VALUES)
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
    <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <FormField label="Nome" htmlFor="account-name" error={errors.name?.message}>
        <Input id="account-name" placeholder="Conta corrente Nubank" {...register('name')} />
      </FormField>
      <FormField
        label="Tipo"
        htmlFor="account-type"
        error={errors.type?.message}
        hint={
          watch('type') === 'INVESTMENT'
            ? 'Aplicações e resgates são transferências; o rendimento vem do valor de mercado que você informar.'
            : undefined
        }
      >
        <Select id="account-type" {...register('type')}>
          {Object.entries(ACCOUNT_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Uso" htmlFor="account-scope" error={errors.scope?.message}>
        <Select id="account-scope" {...register('scope')}>
          {Object.entries(ACCOUNT_SCOPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField
        label="Moeda"
        htmlFor="account-currency"
        error={errors.currency?.message}
        hint={
          currencyLocked
            ? 'A conta já tem lançamentos: para outra moeda, crie uma nova conta.'
            : currency !== 'BRL'
              ? 'Saldo e lançamentos ficam nesta moeda; os totais do app são convertidos para reais pela PTAX.'
              : undefined
        }
      >
        <Select id="account-currency" disabled={currencyLocked} {...register('currency')}>
          {CURRENCIES.map((value) => (
            <option key={value} value={value}>
              {CURRENCY_LABELS[value]}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Saldo inicial" htmlFor="account-balance" error={errors.initialBalance?.message}>
        <Input id="account-balance" type="number" step="0.01" disabled={isEditing} {...register('initialBalance')} />
      </FormField>
      {isEditing && (
        <FormField label="Saldo atual" htmlFor="account-current-balance">
          <Input
            id="account-current-balance"
            value={formatCurrency(account.currentBalance, account.currency)}
            disabled
            readOnly
          />
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
