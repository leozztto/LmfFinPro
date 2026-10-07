import { useState } from 'react'
import { Button, Checkbox, Modal } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { ACCOUNT_TYPE_LABELS } from '@/features/accounts/types'
import { usePersonalAccounts, useShareAccounts } from '../hooks/useShareAccounts'
import type { Household } from '../types'

interface ShareAccountsModalProps {
  household: Household
  onClose: () => void
}

/**
 * Passa contas do espaço pessoal para o grupo, com todo o histórico. Monte só quando for abrir: o
 * estado (contas marcadas, erro) começa do zero a cada vez.
 */
export function ShareAccountsModal({ household, onClose }: ShareAccountsModalProps) {
  const { personal } = useHousehold()
  const { data: accounts, isLoading, isError } = usePersonalAccounts(personal?.id ?? null, true)
  const shareAccounts = useShareAccounts(household.id)
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [selected, setSelected] = useState<Set<number>>(new Set())

  const allSelected = accounts !== undefined && accounts.length > 0 && selected.size === accounts.length

  function toggle(accountId: number) {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(accountId)) next.delete(accountId)
      else next.add(accountId)
      return next
    })
  }

  function toggleAll() {
    setSelected(allSelected || !accounts ? new Set() : new Set(accounts.map((account) => account.id)))
  }

  async function handleShare() {
    const count = selected.size
    const confirmed = await confirm({
      title: 'Compartilhar contas',
      message:
        `${count === 1 ? 'A conta escolhida' : `As ${count} contas escolhidas`} e todo o histórico ` +
        `${count === 1 ? 'dela' : 'delas'} passam a ser do grupo "${household.name}" e saem dos seus dados ` +
        'pessoais. Todos os membros poderão ver e editar. Isso não pode ser desfeito por aqui.',
      confirmLabel: 'Compartilhar',
      variant: 'brand',
    })
    if (!confirmed) return

    shareAccounts.mutate([...selected], {
      onSuccess: (result) => {
        const accountsText = `${result.accounts} ${result.accounts === 1 ? 'conta' : 'contas'}`
        const transactionsText = `${result.transactions} ${result.transactions === 1 ? 'transação' : 'transações'}`
        showToast(`${accountsText} e ${transactionsText} agora são do grupo.`, 'success')
        onClose()
      },
    })
  }

  return (
    <Modal open onClose={onClose} title={`Compartilhar contas com "${household.name}"`} size="lg">
      <div className="space-y-4">
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Escolha quais contas do seu espaço pessoal vão para o grupo. As transações, importações, recorrências e anexos
          de cada conta vão junto. Uma transferência com outra conta sua não obriga a compartilhá-la: o grupo vê só o
          lado da conta compartilhada. Já uma meta de economia liga duas contas, então as duas precisam ir juntas.
        </p>

        {isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando suas contas...</p>}
        {isError && <p className="text-sm text-red-600">Não foi possível carregar as suas contas.</p>}
        {accounts && accounts.length === 0 && (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Você não tem contas no espaço pessoal para compartilhar.
          </p>
        )}

        {accounts && accounts.length > 0 && (
          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm font-medium text-zinc-700 dark:text-zinc-200">
              <Checkbox checked={allSelected} onChange={toggleAll} />
              Selecionar todas
            </label>
            <ul className="max-h-72 space-y-2 overflow-y-auto pr-1">
              {accounts.map((account) => (
                <li key={account.id}>
                  <label className="flex min-w-0 cursor-pointer items-center gap-3 rounded-lg border border-zinc-200 bg-white px-3 py-2 dark:border-zinc-700 dark:bg-zinc-900">
                    <Checkbox checked={selected.has(account.id)} onChange={() => toggle(account.id)} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-zinc-800 dark:text-zinc-100">
                        {account.name}
                      </span>
                      <span className="block truncate text-xs text-zinc-500 dark:text-zinc-400">
                        {ACCOUNT_TYPE_LABELS[account.type]}
                      </span>
                    </span>
                    <span className="shrink-0 text-sm text-zinc-700 dark:text-zinc-200">
                      {formatCurrency(account.currentBalance, account.currency)}
                    </span>
                  </label>
                </li>
              ))}
            </ul>
          </div>
        )}

        {shareAccounts.isError && (
          <p role="alert" className="text-sm text-red-600">
            {shareAccounts.error instanceof ApiError
              ? shareAccounts.error.message
              : 'Não foi possível compartilhar as contas.'}
          </p>
        )}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" className="w-full sm:w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant="brand"
            className="w-full sm:w-auto"
            disabled={selected.size === 0 || shareAccounts.isPending}
            onClick={handleShare}
          >
            {shareAccounts.isPending
              ? 'Compartilhando...'
              : selected.size === 0
                ? 'Compartilhar'
                : `Compartilhar ${selected.size} ${selected.size === 1 ? 'conta' : 'contas'}`}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
