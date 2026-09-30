import type { ReactNode } from 'react'
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
    // Container query: o card fica em meia tela no desktop, então o layout segue a largura do card, não a da tela.
    <form onSubmit={handleSubmit(onSubmit)} className="@container space-y-5">
      <SettingsSection title="Base do cálculo">
        <FormField
          label="Calcular a partir de"
          htmlFor="pro-labore-base"
          error={errors.calculationBase?.message}
          hint={
            isMonthIncome
              ? 'Receitas PJ do mês − despesas, imposto e reserva. Nunca passa do que há nas contas PJ.'
              : 'Saldo das contas PJ − contas a pagar, imposto e colchão de caixa.'
          }
        >
          <Select id="pro-labore-base" {...register('calculationBase')}>
            {Object.entries(CALCULATION_BASE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>

        {isMonthIncome ? (
          <FormField
            label="Reserva da empresa (%)"
            htmlFor="pro-labore-reserve"
            error={errors.reservePercent?.message}
            hint="Percentual das receitas que fica na empresa."
          >
            <Input id="pro-labore-reserve" type="number" step="0.01" inputMode="decimal" {...register('reservePercent')} />
          </FormField>
        ) : (
          <FormField
            label="Colchão de caixa (meses)"
            htmlFor="pro-labore-cushion"
            error={errors.cashCushionMonths?.message}
            hint="Meses de despesa média que ficam na empresa."
          >
            <Input id="pro-labore-cushion" type="number" inputMode="numeric" {...register('cashCushionMonths')} />
          </FormField>
        )}
      </SettingsSection>

      <SettingsSection title="Imposto da empresa">
        <FormField
          label="Alíquota"
          htmlFor="pro-labore-tax-mode"
          error={errors.taxMode?.message}
          hint={isManualTax ? undefined : 'Usa a da caixinha do imposto ou, sem ela, a do seu regime.'}
        >
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
      </SettingsSection>

      <SettingsSection title="Encargos do pró-labore">
        <FormField
          label="INSS e IRRF"
          htmlFor="pro-labore-withholding"
          error={errors.withholdingMode?.message}
          hint={
            withholdingDisabled
              ? 'O valor calculado é tratado como líquido.'
              : 'Automático: só Simples Nacional e Lucro Presumido. MEI e autônomo não têm retenção.'
          }
        >
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
            label="INSS patronal (%)"
            htmlFor="pro-labore-employer-inss"
            error={errors.employerInssPercent?.message}
            hint="Vazio: 20% no Lucro Presumido e 0% nos demais."
          >
            <Input
              id="pro-labore-employer-inss"
              type="number"
              step="0.01"
              inputMode="decimal"
              placeholder="Automático"
              {...register('employerInssPercent')}
            />
          </FormField>
        )}
      </SettingsSection>

      <SettingsSection title="Valor fixo">
        <FormField
          label="Pró-labore fixo mensal, bruto (R$)"
          htmlFor="pro-labore-fixed"
          error={errors.fixedAmount?.message}
          hint="Em branco: o cálculo sugere todo o disponível do mês."
        >
          <Input
            id="pro-labore-fixed"
            type="number"
            step="0.01"
            inputMode="decimal"
            placeholder="Opcional"
            {...register('fixedAmount')}
          />
        </FormField>
      </SettingsSection>

      <div className="flex flex-col gap-3 border-t border-zinc-200 pt-4 dark:border-zinc-700 @md:flex-row @md:items-center @md:justify-between">
        <p className="text-xs text-zinc-500 dark:text-zinc-400">
          Valores de referência de 2026. Confira com seu contador.
        </p>
        <Button
          type="submit"
          variant="secondary"
          className="w-full shrink-0 @md:w-auto"
          disabled={!isDirty || updateSettings.isPending}
        >
          {updateSettings.isPending ? 'Salvando...' : 'Salvar configuração'}
        </Button>
      </div>
    </form>
  )
}

function SettingsSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="space-y-3">
      <legend className="text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">{title}</legend>
      <div className="grid grid-cols-1 gap-4 @md:grid-cols-2">{children}</div>
    </fieldset>
  )
}
