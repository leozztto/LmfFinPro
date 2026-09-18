import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useCreateTransaction } from '../hooks/useCreateTransaction'
import { transactionSchema, type TransactionFormValues } from '../schemas'
import { TRANSACTION_TYPE_LABELS } from '../types'
import { getCurrentIsoDate } from '@/shared/format/date'

interface TransactionFormProps {
  onSuccess?: () => void
}

export function TransactionForm({ onSuccess }: TransactionFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const createTransaction = useCreateTransaction()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      type: 'EXPENSE',
      transactionDate: getCurrentIsoDate(),
    },
  })

  const selectedCategoryId = watch('categoryId')
  const selectedCategory = categories?.find((category) => String(category.id) === String(selectedCategoryId))

  // A categoria já define se é receita ou despesa, então o tipo é derivado dela.
  useEffect(() => {
    if (selectedCategory) {
      setValue('type', selectedCategory.type)
    }
  }, [selectedCategory, setValue])

  async function onSubmit(values: TransactionFormValues) {
    try {
      await createTransaction.mutateAsync(values)
      reset({
        accountId: values.accountId,
        description: '',
        amount: 0,
        type: 'EXPENSE',
        transactionDate: getCurrentIsoDate(),
      })
      showToast('Transação lançada com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível lançar a transação.')
    }
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Cadastre uma conta antes de lançar transações.
      </p>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Conta" htmlFor="transaction-account" error={errors.accountId?.message}>
        <Select id="transaction-account" {...register('accountId')}>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Categoria (opcional)" htmlFor="transaction-category" error={errors.categoryId?.message}>
        <Select id="transaction-category" {...register('categoryId')}>
          <option value="">Sem categoria</option>
          {categories?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name} ({TRANSACTION_TYPE_LABELS[category.type]})
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Tipo" htmlFor="transaction-type" error={errors.type?.message}>
        <Select id="transaction-type" disabled={Boolean(selectedCategory)} {...register('type')}>
          {Object.entries(TRANSACTION_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Data" htmlFor="transaction-date" error={errors.transactionDate?.message}>
        <Input id="transaction-date" type="date" {...register('transactionDate')} />
      </FormField>
      <div className="sm:col-span-2">
        <FormField label="Descrição" htmlFor="transaction-description" error={errors.description?.message}>
          <Input id="transaction-description" placeholder="Pagamento cliente X" {...register('description')} />
        </FormField>
      </div>
      <FormField label="Valor" htmlFor="transaction-amount" error={errors.amount?.message}>
        <Input id="transaction-amount" type="number" step="0.01" {...register('amount')} />
      </FormField>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={createTransaction.isPending} className="w-full">
          {createTransaction.isPending ? 'Salvando...' : 'Lançar transação'}
        </Button>
      </div>
    </form>
  )
}
