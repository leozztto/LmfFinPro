import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card, IconButton, Modal, Select, StatCard } from '@/shared/ui'
import { PencilIcon, PlusIcon, TrashIcon, TrendingUpIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { ACCOUNT_SCOPE_LABELS } from '@/features/accounts/types'
import { AccountValuationsPanel } from '@/features/accounts/components/AccountValuationsPanel'
import { useDeleteDebt, useNetWorth } from '../hooks/useNetWorth'
import { DEBT_TYPE_LABELS, type NetWorth, type NetWorthDebtRow, type NetWorthInvestmentRow } from '../types'
import { formatGainRate, formatSignedCurrency } from '../utils'
import { DebtBalancesPanel } from './DebtBalancesPanel'
import { DebtForm } from './DebtForm'
import { NetWorthChart } from './NetWorthChart'

const PERIOD_OPTIONS = [6, 12, 24, 36] as const

const gainClassName = (value: number) =>
  value > 0 ? 'text-[#5ab482]' : value < 0 ? 'text-[#f06464]' : 'text-zinc-500 dark:text-zinc-400'

export function NetWorthPage() {
  const [months, setMonths] = useState<number>(12)
  const { data: netWorth, isLoading, isError } = useNetWorth(months)
  const [valuing, setValuing] = useState<NetWorthInvestmentRow | null>(null)
  const [creatingDebt, setCreatingDebt] = useState(false)
  const [editingDebt, setEditingDebt] = useState<NetWorthDebtRow | null>(null)
  const [updatingDebt, setUpdatingDebt] = useState<NetWorthDebtRow | null>(null)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Patrimônio</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Tudo o que você tem (contas e investimentos) menos o que deve, ao longo do tempo.
          </p>
        </div>
        <label className="flex items-center gap-2 text-sm text-zinc-600 dark:text-zinc-300">
          <span className="shrink-0">Período</span>
          <Select value={months} onChange={(event) => setMonths(Number(event.target.value))} aria-label="Período">
            {PERIOD_OPTIONS.map((option) => (
              <option key={option} value={option}>
                Últimos {option} meses
              </option>
            ))}
          </Select>
        </label>
      </div>

      {isError && (
        <Card>
          <p className="text-sm text-red-600 dark:text-red-400">Não foi possível carregar o patrimônio.</p>
        </Card>
      )}
      {isLoading && <div className="h-80 animate-pulse rounded-xl bg-zinc-100 dark:bg-zinc-800" />}

      {netWorth && (
        <>
          <SummaryCards netWorth={netWorth} />
          {hasForeignCurrency(netWorth) && (
            <p className="text-xs text-zinc-500 dark:text-zinc-400">
              Valores em reais: contas em outra moeda são convertidas pela PTAX do fim de cada mês (no mês atual, pela
              última cotação), então a variação do câmbio aparece no patrimônio.
            </p>
          )}
          <div className="space-y-4">
            <NetWorthChart history={netWorth.history} />
            <div className="grid gap-3 lg:grid-cols-2">
              <InvestmentsCard netWorth={netWorth} onValue={setValuing} />
              <DebtsCard
                netWorth={netWorth}
                onCreate={() => setCreatingDebt(true)}
                onEdit={setEditingDebt}
                onUpdateBalance={setUpdatingDebt}
              />
            </div>
            <AccountsCard netWorth={netWorth} />
          </div>
        </>
      )}

      <Modal
        open={valuing != null}
        onClose={() => setValuing(null)}
        title={valuing ? `Valor de mercado · ${valuing.name}` : 'Valor de mercado'}
      >
        {valuing && <AccountValuationsPanel key={valuing.accountId} accountId={valuing.accountId} />}
      </Modal>
      <Modal open={creatingDebt} onClose={() => setCreatingDebt(false)} title="Nova dívida" size="lg">
        <DebtForm onSuccess={() => setCreatingDebt(false)} />
      </Modal>
      <Modal open={editingDebt != null} onClose={() => setEditingDebt(null)} title="Editar dívida" size="lg">
        {editingDebt && <DebtForm key={editingDebt.debtId} debt={editingDebt} onSuccess={() => setEditingDebt(null)} />}
      </Modal>
      <Modal
        open={updatingDebt != null}
        onClose={() => setUpdatingDebt(null)}
        title={updatingDebt ? `Saldo devedor · ${updatingDebt.name}` : 'Saldo devedor'}
      >
        {updatingDebt && <DebtBalancesPanel key={updatingDebt.debtId} debtId={updatingDebt.debtId} />}
      </Modal>
    </div>
  )
}

function hasForeignCurrency(netWorth: NetWorth): boolean {
  return [...netWorth.accounts, ...netWorth.investments].some((row) => row.currency !== 'BRL')
}

function SummaryCards({ netWorth }: { netWorth: NetWorth }) {
  const { current, changeFromPreviousMonth, investmentGain } = netWorth
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <StatCard
        label="Patrimônio líquido"
        value={formatCurrency(current.netWorth)}
        hint={
          changeFromPreviousMonth == null
            ? undefined
            : `${formatSignedCurrency(changeFromPreviousMonth)} desde o fim do mês passado`
        }
      />
      <StatCard label="Contas" value={formatCurrency(current.cash)} hint="Saldo das contas, sem investimentos" />
      <StatCard
        label="Investimentos"
        value={formatCurrency(current.investments)}
        hint={netWorth.investments.length > 0 ? `Rendimento: ${formatSignedCurrency(investmentGain)}` : undefined}
      />
      <StatCard label="Dívidas" value={formatCurrency(current.debts)} hint="Último saldo devedor informado" />
    </div>
  )
}

function InvestmentsCard({
  netWorth,
  onValue,
}: {
  netWorth: NetWorth
  onValue: (investment: NetWorthInvestmentRow) => void
}) {
  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Investimentos</h3>
      <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
        Contas do tipo Investimento. Aplique e resgate por transferência e informe o valor atual de tempos em tempos.
      </p>
      {netWorth.investments.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma conta de investimento. Crie uma em{' '}
          <Link to="/contas" className="font-medium text-[#1ea883] underline dark:text-[#2ad6a5]">
            Contas
          </Link>{' '}
          com o tipo <strong>Investimento</strong>.
        </p>
      ) : (
        <ul className="mt-3 divide-y divide-zinc-200 dark:divide-zinc-700">
          {netWorth.investments.map((investment) => {
            const rate = formatGainRate(investment.gainRate)
            return (
              <li key={investment.accountId} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                <div className="min-w-0">
                  <p className="break-words text-sm font-medium text-zinc-800 dark:text-zinc-100">{investment.name}</p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    Aplicado {formatCurrency(investment.invested, investment.currency)} ·{' '}
                    <span className={gainClassName(investment.gain)}>
                      {formatSignedCurrency(investment.gain, investment.currency)}
                      {rate && ` (${rate})`}
                    </span>
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {investment.lastValuationDate
                      ? `Valor informado em ${formatDateOnlyBr(investment.lastValuationDate)}`
                      : 'Valor de mercado ainda não informado'}{' '}
                    · {ACCOUNT_SCOPE_LABELS[investment.scope]}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className="text-right">
                    <span className="block text-sm font-semibold text-zinc-800 dark:text-zinc-100">
                      {formatCurrency(investment.currentValue, investment.currency)}
                    </span>
                    {investment.currency !== 'BRL' && (
                      <span className="block text-xs text-zinc-500 dark:text-zinc-400">
                        ≈ {formatCurrency(investment.currentValueInBrl)}
                      </span>
                    )}
                  </span>
                  <IconButton icon={TrendingUpIcon} label="Atualizar valor" onClick={() => onValue(investment)} />
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}

function DebtsCard({
  netWorth,
  onCreate,
  onEdit,
  onUpdateBalance,
}: {
  netWorth: NetWorth
  onCreate: () => void
  onEdit: (debt: NetWorthDebtRow) => void
  onUpdateBalance: (debt: NetWorthDebtRow) => void
}) {
  const deleteDebt = useDeleteDebt()
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleDelete(debt: NetWorthDebtRow) {
    const confirmed = await confirm({
      message: `Excluir a dívida "${debt.name}" e todo o histórico de saldo devedor? As parcelas já lançadas como despesa continuam.`,
    })
    if (!confirmed) return
    deleteDebt.mutate(debt.debtId, {
      onSuccess: () => showToast('Dívida excluída.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir a dívida.'),
    })
  }

  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Dívidas</h3>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            Financiamentos, empréstimos e cartões. As parcelas seguem como despesas.
          </p>
        </div>
        <Button onClick={onCreate} aria-label="Nova dívida" title="Nova dívida" className="shrink-0 px-3">
          <PlusIcon />
        </Button>
      </div>
      {netWorth.debts.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500 dark:text-zinc-400">Nenhuma dívida cadastrada.</p>
      ) : (
        <ul className="mt-3 divide-y divide-zinc-200 dark:divide-zinc-700">
          {netWorth.debts.map((debt) => (
            <li key={debt.debtId} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
              <div className="min-w-0">
                <p className="break-words text-sm font-medium text-zinc-800 dark:text-zinc-100">{debt.name}</p>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  {[DEBT_TYPE_LABELS[debt.type], debt.creditor].filter(Boolean).join(' · ')}
                </p>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  {debt.currentBalance === 0
                    ? 'Quitada'
                    : debt.lastBalanceDate && `Saldo informado em ${formatDateOnlyBr(debt.lastBalanceDate)}`}
                </p>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-2">
                <span className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">
                  {formatCurrency(debt.currentBalance)}
                </span>
                <div className="flex gap-1">
                  <IconButton icon={TrendingUpIcon} label="Atualizar saldo devedor" onClick={() => onUpdateBalance(debt)} />
                  <IconButton icon={PencilIcon} label="Editar" onClick={() => onEdit(debt)} />
                  <IconButton
                    icon={TrashIcon}
                    label="Excluir"
                    onClick={() => handleDelete(debt)}
                    disabled={deleteDebt.isPending}
                  />
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

function AccountsCard({ netWorth }: { netWorth: NetWorth }) {
  if (netWorth.accounts.length === 0) return null
  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Contas</h3>
      <ul className="mt-3 grid gap-x-6 sm:grid-cols-2">
        {netWorth.accounts.map((account) => (
          <li
            key={account.accountId}
            className="flex items-center justify-between gap-3 border-b border-zinc-200 py-2 text-sm dark:border-zinc-700"
          >
            <span className="min-w-0">
              <span className="block truncate text-zinc-800 dark:text-zinc-100">{account.name}</span>
              <span className="block text-xs text-zinc-500 dark:text-zinc-400">{ACCOUNT_SCOPE_LABELS[account.scope]}</span>
            </span>
            <span className="shrink-0 text-right">
              <span className="block font-medium text-zinc-800 dark:text-zinc-100">
                {formatCurrency(account.balance, account.currency)}
              </span>
              {account.currency !== 'BRL' && (
                <span className="block text-xs text-zinc-500 dark:text-zinc-400">
                  ≈ {formatCurrency(account.balanceInBrl)}
                </span>
              )}
            </span>
          </li>
        ))}
      </ul>
    </Card>
  )
}
