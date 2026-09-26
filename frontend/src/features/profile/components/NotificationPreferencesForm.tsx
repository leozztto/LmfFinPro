import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Checkbox, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useUpdateNotificationPreferences } from '../hooks/useNotificationPreferences'
import type { NotificationPreferences } from '../types'

const DAYS_MESSAGE = 'Informe de 0 a 15 dias'

const notificationPreferencesSchema = z.object({
  billsEnabled: z.boolean(),
  billDaysBefore: z
    .number({ invalid_type_error: DAYS_MESSAGE })
    .int(DAYS_MESSAGE)
    .min(0, DAYS_MESSAGE)
    .max(15, DAYS_MESSAGE),
  budgetsEnabled: z.boolean(),
  dasEnabled: z.boolean(),
})

const TOGGLES = [
  {
    name: 'billsEnabled',
    label: 'Contas a vencer',
    description: 'Despesas pendentes que vencem nos próximos dias.',
  },
  {
    name: 'budgetsEnabled',
    label: 'Orçamentos',
    description: 'Quando o gasto do mês passa de 80% e de 100% do limite.',
  },
  {
    name: 'dasEnabled',
    label: 'DAS',
    description: 'Lembrete do vencimento no dia 20, para MEI e Simples Nacional.',
  },
] as const

interface NotificationPreferencesFormProps {
  preferences: NotificationPreferences
}

export function NotificationPreferencesForm({ preferences }: NotificationPreferencesFormProps) {
  const { showToast } = useToast()
  const updatePreferences = useUpdateNotificationPreferences()
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<NotificationPreferences>({
    resolver: zodResolver(notificationPreferencesSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: preferences,
  })

  // A antecedência só vale para contas e DAS; sem nenhum dos dois ligado, o campo não tem efeito.
  const daysBeforeDisabled = !watch('billsEnabled') && !watch('dasEnabled')

  async function onSubmit(values: NotificationPreferences) {
    const saved = await updatePreferences.mutateAsync(values)
    reset(saved)
    showToast('Preferências de notificação salvas.', 'success')
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <ul className="divide-y divide-zinc-200 dark:divide-zinc-700">
        {TOGGLES.map((toggle) => (
          <li key={toggle.name}>
            <label htmlFor={toggle.name} className="flex cursor-pointer items-start justify-between gap-4 py-3">
              <span className="min-w-0">
                <span className="block text-sm font-medium text-zinc-800 dark:text-zinc-100">{toggle.label}</span>
                <span className="block text-sm text-zinc-500 dark:text-zinc-400">{toggle.description}</span>
              </span>
              <Checkbox id={toggle.name} className="mt-0.5 shrink-0" {...register(toggle.name)} />
            </label>
          </li>
        ))}
      </ul>

      <div className="sm:max-w-xs">
        <FormField
          label="Avisar com quantos dias de antecedência"
          htmlFor="billDaysBefore"
          error={errors.billDaysBefore?.message}
        >
          <Input
            id="billDaysBefore"
            type="number"
            inputMode="numeric"
            min={0}
            max={15}
            disabled={daysBeforeDisabled}
            {...register('billDaysBefore', { valueAsNumber: true })}
          />
        </FormField>
        <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
          Vale para contas a vencer e para o DAS. Use 0 para ser avisado só no dia.
        </p>
      </div>

      {updatePreferences.isError && (
        <p className="text-sm text-red-600">
          {updatePreferences.error instanceof ApiError
            ? updatePreferences.error.message
            : 'Não foi possível salvar as preferências.'}
        </p>
      )}

      <div className="flex justify-end">
        <Button
          type="submit"
          variant="brand"
          className="w-full sm:w-auto"
          disabled={!isDirty || updatePreferences.isPending}
        >
          {updatePreferences.isPending ? 'Salvando...' : 'Salvar preferências'}
        </Button>
      </div>
    </form>
  )
}
