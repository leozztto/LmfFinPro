import { useEffect, useState } from 'react'
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
import { CURRENCIES, CURRENCY_LABELS, formatCurrency, type Currency } from '@/shared/format/currency'
import { useConversion } from '@/shared/currency/useConversion'
import { AttachmentDropzone } from '@/features/attachments/components/AttachmentDropzone'
import { PendingAttachmentList } from '@/features/attachments/components/PendingAttachmentList'
import { usePendingAttachments } from '@/features/attachments/hooks/usePendingAttachments'
import { useUploadPendingAttachments } from '@/features/attachments/hooks/useTransactionAttachments'
import { MAX_ATTACHMENTS_PER_TRANSACTION } from '@/features/attachments/utils'
import { TagInput } from '@/features/tags/components/TagInput'

interface TransactionFormProps {
  onSuccess?: () => void
}

function conversionHint(conversion: ReturnType<typeof useConversion>, accountCurrency: Currency): string {
  if (conversion.isError) return 'Cotação indisponível agora: informe o valor cobrado.'
  if (conversion.data) {
    const rate = formatCurrency(conversion.data.rate, accountCurrency)
    return `PTAX: 1 ${conversion.data.from} = ${rate}. Ajuste para o valor que o banco cobrou (spread, IOF).`
  }
  if (conversion.isFetching) return 'Buscando a cotação...'
  return 'Informe o valor na moeda da operação para sugerir a conversão.'
}

export function TransactionForm({ onSuccess }: TransactionFormProps) {
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const createTransaction = useCreateTransaction()
  const uploadPending = useUploadPendingAttachments()
  const pendingAttachments = usePendingAttachments()
  const [tagNames, setTagNames] = useState<string[]>([])
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

  // Operação em outra moeda: o valor na conta é sugerido pela PTAX e pode ser ajustado para o que
  // o banco realmente cobrou (spread, IOF). Depois de editado à mão, a sugestão não o sobrescreve.
  const selectedAccountId = watch('accountId')
  const selectedAccount =
    accounts?.find((account) => String(account.id) === String(selectedAccountId)) ?? accounts?.[0]
  const accountCurrency: Currency = selectedAccount?.currency ?? 'BRL'
  const originalCurrency = watch('originalCurrency') as Currency | '' | undefined
  const isForeignOperation = !!originalCurrency && originalCurrency !== accountCurrency
  const transactionDate = watch('transactionDate')
  const conversion = useConversion(
    isForeignOperation ? originalCurrency : undefined,
    accountCurrency,
    Number(watch('originalAmount')) || 0,
    transactionDate,
  )
  const [amountEditedByHand, setAmountEditedByHand] = useState(false)

  useEffect(() => {
    if (originalCurrency && originalCurrency === accountCurrency) {
      setValue('originalCurrency', undefined)
    }
  }, [originalCurrency, accountCurrency, setValue])

  useEffect(() => {
    if (isForeignOperation && conversion.data && !amountEditedByHand) {
      setValue('amount', conversion.data.amount, { shouldValidate: true })
    }
  }, [isForeignOperation, conversion.data, amountEditedByHand, setValue])

  async function onSubmit(values: TransactionFormValues) {
    const foreign = isForeignOperation
      ? { originalCurrency: values.originalCurrency, originalAmount: values.originalAmount }
      : { originalCurrency: undefined, originalAmount: undefined }
    let transactionId: number
    try {
      transactionId = (await createTransaction.mutateAsync({ ...values, ...foreign, tagNames })).id
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
      originalCurrency: undefined,
      originalAmount: undefined,
    })
    setAmountEditedByHand(false)
    pendingAttachments.clear()
    setTagNames([])
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
              {account.currency === 'BRL' ? account.name : `${account.name} (${account.currency})`}
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
      <FormField label="Moeda da operação" htmlFor="transaction-original-currency">
        <Select
          id="transaction-original-currency"
          {...register('originalCurrency', { onChange: () => setAmountEditedByHand(false) })}
        >
          <option value="">{CURRENCY_LABELS[accountCurrency]} · moeda da conta</option>
          {CURRENCIES.filter((currency) => currency !== accountCurrency).map((currency) => (
            <option key={currency} value={currency}>
              {CURRENCY_LABELS[currency]}
            </option>
          ))}
        </Select>
      </FormField>
      {isForeignOperation && (
        <FormField
          label={`Valor em ${originalCurrency}`}
          htmlFor="transaction-original-amount"
          error={errors.originalAmount?.message}
        >
          <Input id="transaction-original-amount" type="number" step="0.01" {...register('originalAmount')} />
        </FormField>
      )}
      <FormField
        label={
          isForeignOperation
            ? `Valor cobrado na conta (${accountCurrency})`
            : accountCurrency === 'BRL'
              ? 'Valor'
              : `Valor (${accountCurrency})`
        }
        htmlFor="transaction-amount"
        error={errors.amount?.message}
        hint={isForeignOperation ? conversionHint(conversion, accountCurrency) : undefined}
      >
        <Input
          id="transaction-amount"
          type="number"
          step="0.01"
          {...register('amount', { onChange: () => setAmountEditedByHand(true) })}
        />
      </FormField>
      <div className="min-w-0 sm:col-span-2">
        <FormField
          label="Tags (opcional)"
          htmlFor="transaction-tags"
          hint="Ex.: #site-acme, #dedutível. Enter ou vírgula adiciona; tag nova é criada na hora."
        >
          <TagInput
            id="transaction-tags"
            value={tagNames}
            onChange={setTagNames}
            placeholder="Digite uma tag"
            disabled={isSaving}
          />
        </FormField>
      </div>
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
