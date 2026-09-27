import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, IconButton, Input } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr, getCurrentIsoDate } from '@/shared/format/date'
import { useDebtBalances, useDeleteDebtBalance, useSaveDebtBalance } from '../hooks/useNetWorth'
import { debtBalanceSchema, type DebtBalanceFormValues } from '../schemas'

/** Informar o saldo devedor atual de uma dívida e ver/excluir os já informados. */
export function DebtBalancesPanel({ debtId }: { debtId: number }) {
  const { data: balances, isLoading } = useDebtBalances(debtId)
  const saveBalance = useSaveDebtBalance()
  const deleteBalance = useDeleteDebtBalance()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DebtBalanceFormValues>({
    resolver: zodResolver(debtBalanceSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { balanceDate: getCurrentIsoDate() },
  })

  async function onSubmit(values: DebtBalanceFormValues) {
    try {
      await saveBalance.mutateAsync({ debtId, input: values })
      showToast(values.balance === 0 ? 'Dívida marcada como quitada.' : 'Saldo devedor atualizado.', 'success')
      reset({ balanceDate: getCurrentIsoDate() })
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível salvar o saldo.')
    }
  }

  async function handleDelete(balanceId: number, date: string) {
    const confirmed = await confirm({ message: `Excluir o saldo informado em ${formatDateOnlyBr(date)}?` })
    if (!confirmed) return
    deleteBalance.mutate(
      { debtId, balanceId },
      {
        onSuccess: () => showToast('Saldo excluído.', 'success'),
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir o saldo.'),
      },
    )
  }

  return (
    <div className="space-y-5">
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Informe quanto ainda falta pagar (pelo extrato do banco). Saldo zero marca a dívida como quitada.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-start">
        <FormField label="Data" htmlFor="debt-balance-date" error={errors.balanceDate?.message}>
          <Input id="debt-balance-date" type="date" max={getCurrentIsoDate()} {...register('balanceDate')} />
        </FormField>
        <FormField label="Saldo devedor (R$)" htmlFor="debt-balance-value" error={errors.balance?.message}>
          <Input id="debt-balance-value" type="number" step="0.01" inputMode="decimal" {...register('balance')} />
        </FormField>
        <Button type="submit" variant="brand" className="sm:mt-6" disabled={saveBalance.isPending}>
          {saveBalance.isPending ? 'Salvando...' : 'Salvar'}
        </Button>
      </form>

      <div>
        <h4 className="text-xs font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
          Saldos informados
        </h4>
        {isLoading ? (
          <p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">Carregando…</p>
        ) : (
          <ul className="mt-2 max-h-64 divide-y divide-zinc-200 overflow-y-auto pr-1 dark:divide-zinc-700">
            {(balances ?? []).map((balance) => (
              <li key={balance.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span className="text-zinc-600 dark:text-zinc-300">{formatDateOnlyBr(balance.balanceDate)}</span>
                <span className="flex items-center gap-2">
                  <span className="font-medium text-zinc-800 dark:text-zinc-100">{formatCurrency(balance.balance)}</span>
                  <IconButton
                    icon={TrashIcon}
                    label="Excluir saldo"
                    onClick={() => handleDelete(balance.id, balance.balanceDate)}
                    disabled={deleteBalance.isPending || (balances?.length ?? 0) <= 1}
                    title={
                      (balances?.length ?? 0) <= 1
                        ? 'A dívida precisa de pelo menos um saldo. Para quitá-la, informe saldo zero.'
                        : 'Excluir saldo'
                    }
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
