import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import type { Account } from '@/features/accounts/types'
import { useCreateTransfer } from '../hooks/useCreateTransfer'
import { transferSchema, type TransferFormValues } from '../schemas'
import { getCurrentIsoDate } from '@/shared/format/date'
import { formatCurrency } from '@/shared/format/currency'
import { useConversion } from '@/shared/currency/useConversion'

export interface TransferFormInitialValues {
  fromAccountId?: number
  toAccountId?: number
  amount?: number
  description?: string
}

interface TransferFormProps {
  onSuccess?: () => void
  /** Pré-preenchimento (ex.: "Pagar pró-labore" já traz as contas PJ/PF e o valor sugerido). */
  initialValues?: TransferFormInitialValues
}

function accountLabel(account: Account): string {
  return account.currency === 'BRL' ? account.name : `${account.name} (${account.currency})`
}

export function TransferForm({ onSuccess, initialValues }: TransferFormProps) {
  const { data: accounts } = useAccounts()
  const createTransfer = useCreateTransfer()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = useForm<TransferFormValues>({
    resolver: zodResolver(transferSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      ...initialValues,
      transferDate: getCurrentIsoDate(),
    },
  })

  // Entre moedas diferentes o destino recebe outro valor: sugerido pela PTAX e editável, para
  // registrar o câmbio efetivo (spread e taxas da remessa).
  const findAccount = (id: unknown) => accounts?.find((account) => String(account.id) === String(id))
  const fromAccount = findAccount(watch('fromAccountId'))
  const toAccount = findAccount(watch('toAccountId'))
  const isCrossCurrency = !!fromAccount && !!toAccount && fromAccount.currency !== toAccount.currency
  const conversion = useConversion(
    isCrossCurrency ? fromAccount.currency : undefined,
    toAccount?.currency,
    Number(watch('amount')) || 0,
    watch('transferDate'),
  )
  const [receivedEditedByHand, setReceivedEditedByHand] = useState(false)

  useEffect(() => {
    if (isCrossCurrency && conversion.data && !receivedEditedByHand) {
      setValue('receivedAmount', conversion.data.amount, { shouldValidate: true })
    }
  }, [isCrossCurrency, conversion.data, receivedEditedByHand, setValue])

  async function onSubmit(values: TransferFormValues) {
    if (isCrossCurrency && !values.receivedAmount) {
      setError('receivedAmount', { message: 'informe o valor que entrou na conta de destino' })
      return
    }
    try {
      await createTransfer.mutateAsync({
        fromAccountId: values.fromAccountId,
        toAccountId: values.toAccountId,
        amount: values.amount,
        transferDate: values.transferDate,
        description: values.description || undefined,
        receivedAmount: isCrossCurrency ? values.receivedAmount : undefined,
      })
      reset({
        amount: 0,
        description: '',
        transferDate: getCurrentIsoDate(),
        receivedAmount: undefined,
      })
      setReceivedEditedByHand(false)
      showToast('Transferência realizada com sucesso.', 'success')
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

  const receivedHint = conversion.isError
    ? 'Cotação indisponível agora: informe o valor que entrou.'
    : conversion.data && toAccount
      ? `PTAX: 1 ${conversion.data.from} = ${formatCurrency(conversion.data.rate, toAccount.currency)}. Ajuste para o valor que realmente entrou.`
      : 'Informe o valor enviado para sugerir a conversão.'

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <FormField label="Conta de origem" htmlFor="transfer-from-account" error={errors.fromAccountId?.message}>
        <Select id="transfer-from-account" defaultValue={initialValues?.fromAccountId ?? ''} {...register('fromAccountId')}>
          <option value="" disabled>
            Selecione...
          </option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {accountLabel(account)}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Conta de destino" htmlFor="transfer-to-account" error={errors.toAccountId?.message}>
        <Select id="transfer-to-account" defaultValue={initialValues?.toAccountId ?? ''} {...register('toAccountId')}>
          <option value="" disabled>
            Selecione...
          </option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {accountLabel(account)}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField
        label={isCrossCurrency ? `Valor enviado (${fromAccount.currency})` : 'Valor'}
        htmlFor="transfer-amount"
        error={errors.amount?.message}
      >
        <Input id="transfer-amount" type="number" step="0.01" {...register('amount')} />
      </FormField>
      {isCrossCurrency && (
        <FormField
          label={`Valor recebido (${toAccount.currency})`}
          htmlFor="transfer-received-amount"
          error={errors.receivedAmount?.message}
          hint={receivedHint}
        >
          <Input
            id="transfer-received-amount"
            type="number"
            step="0.01"
            {...register('receivedAmount', { onChange: () => setReceivedEditedByHand(true) })}
          />
        </FormField>
      )}
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
