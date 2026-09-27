import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Label, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useCreateTransaction } from '../hooks/useCreateTransaction'
import { transactionSchema, type TransactionFormValues } from '../schemas'
import { TRANSACTION_STATUS_LABELS, TRANSACTION_TYPE_LABELS } from '../types'
import { getCurrentIsoDate } from '@/shared/format/date'
import { AttachmentDropzone } from '@/features/attachments/components/AttachmentDropzone'
import { PendingAttachmentList } from '@/features/attachments/components/PendingAttachmentList'
import { usePendingAttachments } from '@/features/attachments/hooks/usePendingAttachments'
import { useUploadPendingAttachments } from '@/features/attachments/hooks/useTransactionAttachments'
import { MAX_ATTACHMENTS_PER_TRANSACTION } from '@/features/attachments/utils'

interface TransactionFormProps {
  onSuccess?: () => void
}

export function TransactionForm({ onSuccess }: TransactionFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const createTransaction = useCreateTransaction()
  const uploadPending = useUploadPendingAttachments()
  const pendingAttachments = usePendingAttachments()
  const { showToast } = useToast()
  const isSaving = createTransaction.isPending || uploadPending.isPending

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
    let transactionId: number
    try {
      transactionId = (await createTransaction.mutateAsync(values)).id
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível lançar a transação.')
      return
    }

    // Os comprovantes só podem ser enviados depois que a transação existe. Se algum falhar, a
    // transação continua lançada e o arquivo pode ser anexado depois, pelo clipe na lista.
    const { failed } =
      pendingAttachments.items.length > 0
        ? await uploadPending.mutateAsync({ transactionId, items: pendingAttachments.items })
        : { failed: [] }

    reset({
      accountId: values.accountId,
      description: '',
      amount: 0,
      type: 'EXPENSE',
      transactionDate: getCurrentIsoDate(),
    })
    pendingAttachments.clear()
    if (failed.length === 0) {
      showToast('Transação lançada com sucesso.', 'success')
    } else {
      showToast(
        `Transação lançada, mas ${
          failed.length === 1 ? '1 comprovante não foi enviado' : `${failed.length} comprovantes não foram enviados`
        } (${failed.map((item) => `${item.file.name}: ${item.error}`).join('; ')}). Anexe pelo clipe na lista.`,
      )
    }
    onSuccess?.()
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Cadastre uma conta antes de lançar transações.
      </p>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
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
      <FormField label="Cliente (opcional)" htmlFor="transaction-client" error={errors.clientId?.message}>
        <Select id="transaction-client" {...register('clientId')}>
          <option value="">Sem cliente</option>
          {clients?.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
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
      <FormField label="Situação" htmlFor="transaction-status" error={errors.status?.message}>
        <Select id="transaction-status" {...register('status')}>
          <option value="">Automática (data futura fica pendente)</option>
          {Object.entries(TRANSACTION_STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <div className="sm:col-span-2">
        <FormField label="Descrição" htmlFor="transaction-description" error={errors.description?.message}>
          <Input id="transaction-description" placeholder="Pagamento cliente X" {...register('description')} />
        </FormField>
      </div>
      <FormField label="Valor" htmlFor="transaction-amount" error={errors.amount?.message}>
        <Input id="transaction-amount" type="number" step="0.01" {...register('amount')} />
      </FormField>
      <div className="min-w-0 space-y-2 sm:col-span-2">
        <Label htmlFor="transaction-attachments" className="mb-0">
          Comprovantes (opcional)
        </Label>
        <AttachmentDropzone
          id="transaction-attachments"
          onFilesSelected={pendingAttachments.add}
          disabled={isSaving || pendingAttachments.items.length >= MAX_ATTACHMENTS_PER_TRANSACTION}
        />
        <PendingAttachmentList
          items={pendingAttachments.items}
          rejections={pendingAttachments.rejections}
          onChangeType={pendingAttachments.changeType}
          onRemove={pendingAttachments.remove}
          disabled={isSaving}
        />
      </div>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={isSaving} className="w-full">
          {createTransaction.isPending
            ? 'Salvando...'
            : uploadPending.isPending
              ? 'Enviando comprovantes...'
              : 'Lançar transação'}
        </Button>
      </div>
    </form>
  )
}
