import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { percentToRate, rateToPercent } from '@/features/savings-goals/utils'
import { useUpdateProLaboreSettings } from '../hooks/useProLabore'
import { proLaboreSettingsSchema, type ProLaboreSettingsFormValues } from '../schemas'
import { CALCULATION_BASE_LABELS, TAX_MODE_LABELS, WITHHOLDING_MODE_LABELS, type ProLaboreSettings } from '../types'

function toFormValues(settings: ProLaboreSettings): ProLaboreSettingsFormValues {
  return {
    calculationBase: settings.calculationBase,
    reservePercent: rateToPercent(settings.reserveRate),
    cashCushionMonths: settings.cashCushionMonths,
    taxMode: settings.taxMode,
    manualTaxPercent: settings.manualTaxRate === null ? '' : String(rateToPercent(settings.manualTaxRate)),
    fixedAmount: settings.fixedAmount === null ? '' : String(settings.fixedAmount),
    withholdingMode: settings.withholdingMode,
    employerInssPercent: settings.employerInssRate === null ? '' : String(rateToPercent(settings.employerInssRate)),
  }
}

export function ProLaboreSettingsForm({ settings }: { settings: ProLaboreSettings }) {
  const updateSettings = useUpdateProLaboreSettings()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProLaboreSettingsFormValues>({
    resolver: zodResolver(proLaboreSettingsSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: toFormValues(settings),
  })

  const isMonthIncome = watch('calculationBase') === 'MONTH_INCOME'
  const isManualTax = watch('taxMode') === 'MANUAL'
  const withholdingDisabled = watch('withholdingMode') === 'DISABLED'

  async function onSubmit(values: ProLaboreSettingsFormValues) {
    const input: ProLaboreSettings = {
      calculationBase: values.calculationBase,
      cashCushionMonths: values.cashCushionMonths,
      reserveRate: percentToRate(values.reservePercent),
      taxMode: values.taxMode,
      manualTaxRate: values.manualTaxPercent.trim() === '' ? null : percentToRate(Number(values.manualTaxPercent)),
      fixedAmount: values.fixedAmount.trim() === '' ? null : Number(values.fixedAmount),
      withholdingMode: values.withholdingMode,
      employerInssRate:
        values.employerInssPercent.trim() === '' ? null : percentToRate(Number(values.employerInssPercent)),
    }
    try {
      const summary = await updateSettings.mutateAsync(input)
      reset(toFormValues(summary.settings))
      showToast('Configuração do pró-labore salva.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível salvar a configuração.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Base do cálculo" htmlFor="pro-labore-base" error={errors.calculationBase?.message}>
          <Select id="pro-labore-base" {...register('calculationBase')}>
            {Object.entries(CALCULATION_BASE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>

        {isMonthIncome ? (
          <FormField label="Reserva da empresa (% da receita)" htmlFor="pro-labore-reserve" error={errors.reservePercent?.message}>
            <Input id="pro-labore-reserve" type="number" step="0.01" inputMode="decimal" {...register('reservePercent')} />
          </FormField>
        ) : (
          <FormField
            label="Colchão de caixa (meses de despesa)"
            htmlFor="pro-labore-cushion"
            error={errors.cashCushionMonths?.message}
          >
            <Input id="pro-labore-cushion" type="number" inputMode="numeric" {...register('cashCushionMonths')} />
          </FormField>
        )}

        <FormField label="Imposto" htmlFor="pro-labore-tax-mode" error={errors.taxMode?.message}>
          <Select id="pro-labore-tax-mode" {...register('taxMode')}>
            {Object.entries(TAX_MODE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>

        {isManualTax && (
          <FormField label="Alíquota do imposto (%)" htmlFor="pro-labore-tax-rate" error={errors.manualTaxPercent?.message}>
            <Input id="pro-labore-tax-rate" type="number" step="0.01" inputMode="decimal" {...register('manualTaxPercent')} />
          </FormField>
        )}

        <FormField label="INSS e IRRF sobre o pró-labore" htmlFor="pro-labore-withholding" error={errors.withholdingMode?.message}>
          <Select id="pro-labore-withholding" {...register('withholdingMode')}>
            {Object.entries(WITHHOLDING_MODE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>

        {!withholdingDisabled && (
          <FormField
            label="INSS patronal (%) — vazio = automático"
            htmlFor="pro-labore-employer-inss"
            error={errors.employerInssPercent?.message}
          >
            <Input
              id="pro-labore-employer-inss"
              type="number"
              step="0.01"
              inputMode="decimal"
              placeholder="20% no Lucro Presumido, 0% nos demais"
              {...register('employerInssPercent')}
            />
          </FormField>
        )}

        <div className="sm:col-span-2">
          <FormField label="Pró-labore fixo mensal, bruto (opcional)" htmlFor="pro-labore-fixed" error={errors.fixedAmount?.message}>
            <Input
              id="pro-labore-fixed"
              type="number"
              step="0.01"
              inputMode="decimal"
              placeholder="Deixe em branco para não usar valor fixo"
              {...register('fixedAmount')}
            />
          </FormField>
        </div>
      </div>

      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        {isMonthIncome
          ? 'Parte das receitas PJ recebidas no mês e desconta despesas do mês, imposto e reserva. Nunca sugere mais do que há nas contas PJ.'
          : 'Parte do saldo atual das contas PJ e desconta contas a pagar, imposto e o colchão de caixa.'}{' '}
        No automático, INSS (11% do sócio) e IRRF são calculados para Simples Nacional e Lucro Presumido — MEI e autônomo
        não têm retenção sobre pró-labore. Valores de referência de 2026; confira com seu contador.
      </p>

      <Button
        type="submit"
        variant="secondary"
        className="w-full sm:w-auto"
        disabled={!isDirty || updateSettings.isPending}
      >
        {updateSettings.isPending ? 'Salvando...' : 'Salvar configuração'}
      </Button>
    </form>
  )
}
