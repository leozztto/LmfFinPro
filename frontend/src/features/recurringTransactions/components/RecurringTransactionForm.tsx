import { useEffect, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentIsoDate } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { TRANSACTION_TYPE_LABELS } from '@/features/transactions/types'
import { useCreateRecurringTransaction } from '../hooks/useCreateRecurringTransaction'
import { recurringTransactionSchema, type RecurringTransactionFormValues } from '../schemas'
import { RECURRENCE_FREQUENCY_LABELS } from '../types'

interface RecurringTransactionFormProps {
  onSuccess?: () => void
}

export function RecurringTransactionForm({ onSuccess }: RecurringTransactionFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const createRecurringTransaction = useCreateRecurringTransaction()
  const { showToast } = useToast()

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RecurringTransactionFormValues>({
    resolver: zodResolver(recurringTransactionSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      type: 'EXPENSE',
      frequency: 'MONTHLY',
      startDate: getCurrentIsoDate(),
    },
  })

  // O tipo vem primeiro e filtra as categorias; trocar o tipo limpa uma categoria incompatível.
  const selectedType = watch('type')
  const selectedCategoryId = watch('categoryId')
  const categoriesOfType = useMemo(
    () => categories?.filter((category) => category.type === selectedType) ?? [],
    [categories, selectedType],
  )

  useEffect(() => {
    if (selectedCategoryId && !categoriesOfType.some((category) => String(category.id) === String(selectedCategoryId))) {
      setValue('categoryId', undefined)
    }
  }, [selectedType, selectedCategoryId, categoriesOfType, setValue])

  async function onSubmit(values: RecurringTransactionFormValues) {
    try {
      const created = await createRecurringTransaction.mutateAsync(values)
      showToast(
        created.generatedOccurrences > 0
          ? `Recorrência criada. ${created.generatedOccurrences} lançamento(s) já vencido(s) foram registrados.`
          : 'Recorrência criada com sucesso.',
        'success',
      )
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar a recorrência.')
    }
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">Cadastre uma conta antes de criar lançamentos recorrentes.</p>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <FormField label="Descrição" htmlFor="recurring-description" error={errors.description?.message}>
          <Input id="recurring-description" placeholder="Aluguel, assinatura, mensalidade do cliente X" {...register('description')} />
        </FormField>
      </div>
      <FormField label="Tipo" htmlFor="recurring-type" error={errors.type?.message}>
        <Select id="recurring-type" {...register('type')}>
          {Object.entries(TRANSACTION_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Valor" htmlFor="recurring-amount" error={errors.amount?.message}>
        <Input id="recurring-amount" type="number" step="0.01" inputMode="decimal" {...register('amount')} />
      </FormField>
      <FormField label="Conta" htmlFor="recurring-account" error={errors.accountId?.message}>
        <Select id="recurring-account" {...register('accountId')}>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Categoria (opcional)" htmlFor="recurring-category" error={errors.categoryId?.message}>
        <Select id="recurring-category" {...register('categoryId')}>
          <option value="">Sem categoria</option>
          {categoriesOfType.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Cliente (opcional)" htmlFor="recurring-client" error={errors.clientId?.message}>
        <Select id="recurring-client" {...register('clientId')}>
          <option value="">Sem cliente</option>
          {clients?.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Frequência" htmlFor="recurring-frequency" error={errors.frequency?.message}>
        <Select id="recurring-frequency" {...register('frequency')}>
          {Object.entries(RECURRENCE_FREQUENCY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Primeira ocorrência" htmlFor="recurring-start-date" error={errors.startDate?.message}>
        <Input id="recurring-start-date" type="date" {...register('startDate')} />
      </FormField>
      <FormField label="Termina em (opcional)" htmlFor="recurring-end-date" error={errors.endDate?.message}>
        <Input id="recurring-end-date" type="date" {...register('endDate')} />
      </FormField>
      <p className="text-xs text-zinc-500 dark:text-zinc-400 sm:col-span-2">
        Ocorrências com data até hoje são lançadas assim que a recorrência é criada; as próximas entram
        automaticamente como transações no dia de cada uma. Cada ocorrência entra como pendente até você
        marcá-la como paga na tela de Transações.
      </p>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={createRecurringTransaction.isPending} className="w-full">
          {createRecurringTransaction.isPending ? 'Salvando...' : 'Criar recorrência'}
        </Button>
      </div>
    </form>
  )
}
