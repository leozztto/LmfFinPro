import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, IconButton, Input } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr, getCurrentIsoDate } from '@/shared/format/date'
import {
  useAccountValuations,
  useDeleteAccountValuation,
  useSaveAccountValuation,
} from '../hooks/useAccountValuations'
import { valuationSchema, type ValuationFormValues } from '../schemas'

interface AccountValuationsPanelProps {
  accountId: number
}

/** Informar o valor de mercado de uma conta de investimento e ver/excluir os já informados. */
export function AccountValuationsPanel({ accountId }: AccountValuationsPanelProps) {
  const { data: valuations, isLoading } = useAccountValuations(accountId)
  const saveValuation = useSaveAccountValuation()
  const deleteValuation = useDeleteAccountValuation()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ValuationFormValues>({
    resolver: zodResolver(valuationSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { valuationDate: getCurrentIsoDate() },
  })

  async function onSubmit(values: ValuationFormValues) {
    try {
      await saveValuation.mutateAsync({ accountId, input: values })
      showToast('Valor atualizado.', 'success')
      reset({ valuationDate: getCurrentIsoDate() })
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível salvar o valor.')
    }
  }

  async function handleDelete(valuationId: number, date: string) {
    const confirmed = await confirm({
      message: `Excluir o valor informado em ${formatDateOnlyBr(date)}? O saldo da conta volta a seguir o valor anterior.`,
    })
    if (!confirmed) return
    deleteValuation.mutate(
      { accountId, valuationId },
      {
        onSuccess: () => showToast('Valor excluído.', 'success'),
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir o valor.'),
      },
    )
  }

  return (
    <div className="space-y-5">
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Informe quanto o investimento vale hoje (ex.: o saldo do extrato da corretora). O saldo da conta passa a ser
        esse valor mais as aplicações e resgates lançados depois. A diferença para o que foi aplicado é o rendimento.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-start">
        <FormField label="Data" htmlFor="valuation-date" error={errors.valuationDate?.message}>
          <Input id="valuation-date" type="date" max={getCurrentIsoDate()} {...register('valuationDate')} />
        </FormField>
        <FormField label="Valor atual (R$)" htmlFor="valuation-value" error={errors.value?.message}>
          <Input id="valuation-value" type="number" step="0.01" inputMode="decimal" {...register('value')} />
        </FormField>
        <Button type="submit" variant="brand" className="sm:mt-6" disabled={saveValuation.isPending}>
          {saveValuation.isPending ? 'Salvando...' : 'Salvar'}
        </Button>
      </form>

      <div>
        <h4 className="text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
          Valores informados
        </h4>
        {isLoading ? (
          <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">Carregando…</p>
        ) : !valuations?.length ? (
          <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">
            Nenhum valor informado: o saldo segue só as aplicações e resgates.
          </p>
        ) : (
          <ul className="mt-2 max-h-64 divide-y divide-zinc-200 overflow-y-auto pr-1 dark:divide-zinc-700">
            {valuations.map((valuation) => (
              <li key={valuation.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span className="text-zinc-600 dark:text-zinc-300">{formatDateOnlyBr(valuation.valuationDate)}</span>
                <span className="flex items-center gap-2">
                  <span className="font-medium text-zinc-800 dark:text-zinc-100">{formatCurrency(valuation.value)}</span>
                  <IconButton
                    icon={TrashIcon}
                    label="Excluir valor"
                    onClick={() => handleDelete(valuation.id, valuation.valuationDate)}
                    disabled={deleteValuation.isPending}
                  />
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
