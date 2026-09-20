import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatCurrency } from '@/shared/format/currency'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useTransactions } from '@/features/transactions/hooks/useTransactions'
import { useCreateTaxEstimate } from '../hooks/useCreateTaxEstimate'
import { useSuggestedRate } from '../hooks/useSuggestedRate'
import { taxEstimateSchema, type TaxEstimateFormValues } from '../schemas'
import { TAX_REGIME_LABELS } from '../types'

interface TaxEstimateFormProps {
  onSuccess?: () => void
}

const EMPTY_VALUES: TaxEstimateFormValues = {
  referenceMonth: getCurrentYearMonth(),
  regime: '' as TaxEstimateFormValues['regime'],
  grossRevenue: 0,
  appliedRate: 0,
}

export function TaxEstimateForm({ onSuccess }: TaxEstimateFormProps) {
  const createTaxEstimate = useCreateTaxEstimate()
  const { data: transactions } = useTransactions()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = useForm<TaxEstimateFormValues>({
    resolver: zodResolver(taxEstimateSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: EMPTY_VALUES,
  })

  const referenceMonth = useWatch({ control, name: 'referenceMonth' })
  const regime = useWatch({ control, name: 'regime' })
  const grossRevenue = useWatch({ control, name: 'grossRevenue' })
  const appliedRate = useWatch({ control, name: 'appliedRate' })

  const { data: suggestedRate } = useSuggestedRate(regime, Number(grossRevenue) || 0)

  // Pré-preenche a receita bruta com a soma das receitas (sem transferências) do mês
  // selecionado — o usuário pode ajustar o valor livremente antes de salvar.
  useEffect(() => {
    if (!transactions || !referenceMonth) return
    const income = transactions
      .filter((t) => t.transferId == null && t.type === 'INCOME' && t.transactionDate.startsWith(referenceMonth))
      .reduce((sum, t) => sum + t.amount, 0)
    setValue('grossRevenue', income)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [referenceMonth, transactions])

  useEffect(() => {
    if (suggestedRate != null) setValue('appliedRate', suggestedRate.rate)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [suggestedRate])

  const estimatedValue = (Number(grossRevenue) || 0) * (Number(appliedRate) || 0)

  async function onSubmit(values: TaxEstimateFormValues) {
    try {
      await createTaxEstimate.mutateAsync(values)
      showToast('Estimativa criada com sucesso.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar a estimativa.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-950 dark:text-amber-200">
        Estimativa simplificada e educacional — não substitui a orientação de um contador.
      </p>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Mês de referência" htmlFor="tax-estimate-month" error={errors.referenceMonth?.message}>
          <Input id="tax-estimate-month" type="month" {...register('referenceMonth')} />
        </FormField>
        <FormField label="Regime tributário" htmlFor="tax-estimate-regime" error={errors.regime?.message}>
          <Select id="tax-estimate-regime" defaultValue="" {...register('regime')}>
            <option value="" disabled>
              Selecione...
            </option>
            {Object.entries(TAX_REGIME_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Receita bruta do mês" htmlFor="tax-estimate-revenue" error={errors.grossRevenue?.message}>
          <Input id="tax-estimate-revenue" type="number" step="0.01" {...register('grossRevenue')} />
        </FormField>
        <FormField label="Alíquota aplicada" htmlFor="tax-estimate-rate" error={errors.appliedRate?.message}>
          <Input id="tax-estimate-rate" type="number" step="0.0001" {...register('appliedRate')} />
        </FormField>
      </div>

      <p className="text-sm text-zinc-600 dark:text-zinc-300">
        Valor estimado: <span className="font-semibold text-zinc-900 dark:text-zinc-50">{formatCurrency(estimatedValue)}</span>
      </p>

      <Button type="submit" disabled={createTaxEstimate.isPending} className="w-full">
        {createTaxEstimate.isPending ? 'Salvando...' : 'Salvar estimativa'}
      </Button>
    </form>
  )
}
