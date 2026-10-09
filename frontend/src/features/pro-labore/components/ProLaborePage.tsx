import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card, Modal } from '@/shared/ui'
import { ChevronDownIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr, getCurrentYearMonth } from '@/shared/format/date'
import { FiscalNotice } from '@/features/legal/components/FiscalNotice'
import { TransferForm } from '@/features/transfers/components/TransferForm'
import { formatPercent } from '@/features/savings-goals/utils'
import { useProLabore } from '../hooks/useProLabore'
import { CALCULATION_BASE_LABELS, type ProLaboreSummary } from '../types'
import { ProLaboreSettingsForm } from './ProLaboreSettingsForm'

export function ProLaborePage() {
  const { data: summary, isLoading, isError } = useProLabore()
  const [isPaying, setIsPaying] = useState(false)

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Pró-labore</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Quanto você pode se pagar este mês com o dinheiro da empresa, do jeito que você configurar.
        </p>
      </div>

      <FiscalNotice subject="O pró-labore sugerido, o INSS, o IRRF e o imposto reservado" />

      {isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Calculando...</p>}
      {isError && <p className="text-sm text-red-600">Não foi possível calcular o pró-labore.</p>}

      {summary && !summary.hasBusinessAccounts && (
        <Card>
          <p className="font-medium text-zinc-800 dark:text-zinc-100">Nenhuma conta da empresa cadastrada</p>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Para calcular o pró-labore, marque as contas da empresa com o uso <strong>Empresa (PJ)</strong> na tela de{' '}
            <Link to="/contas" className="font-medium text-[#1ea883] underline dark:text-[#2ad6a5]">
              Contas
            </Link>
            .
          </p>
        </Card>
      )}

      {summary?.hasBusinessAccounts && (
        <div className="space-y-4">
          <SettingsCard summary={summary} />
          <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
            <AvailableCard summary={summary} onPay={() => setIsPaying(true)} />
            <BreakdownCard summary={summary} />
            <WithdrawalsCard summary={summary} />
            <BusinessExpensesCard summary={summary} />
          </div>
        </div>
      )}

      {summary && (
        <Modal open={isPaying} onClose={() => setIsPaying(false)} title="Pagar pró-labore">
          <TransferForm
            onSuccess={() => setIsPaying(false)}
            initialValues={{
              fromAccountId: summary.suggestedFromAccountId ?? undefined,
              toAccountId: summary.suggestedToAccountId ?? undefined,
              amount: summary.suggestedPayment,
              description: `Pró-labore ${getCurrentYearMonth().split('-').reverse().join('/')}`,
            }}
          />
        </Modal>
      )}
    </div>
  )
}

function AvailableCard({ summary, onPay }: { summary: ProLaboreSummary; onPay: () => void }) {
  const canPay = summary.suggestedPayment > 0 && summary.hasPersonalAccounts
  const fixed = summary.settings.fixedAmount

  return (
    <Card className="flex flex-col gap-4">
      <div>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Você pode se pagar este mês{summary.withholdingApplied && ' (líquido, já sem INSS e IRRF)'}
        </p>
        <p className="mt-1 text-3xl font-semibold text-zinc-800 dark:text-zinc-100">
          {formatCurrency(summary.availableToWithdraw)}
        </p>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          Já retirado este mês: {formatCurrency(summary.withdrawnThisMonth)}
        </p>
      </div>

      {summary.withholdingApplied && summary.payroll.taxesToCollect > 0 && (
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          Deixe {formatCurrency(summary.payroll.taxesToCollect)} na conta PJ para as guias de INSS e IRRF do
          pró-labore.
        </p>
      )}

      {fixed !== null && summary.fixedRemaining !== null && (
        <div
          className={`rounded-lg p-3 text-sm ${
            summary.fixedCovered
              ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-300'
              : 'bg-amber-50 text-amber-800 dark:bg-amber-500/10 dark:text-amber-300'
          }`}
        >
          <p className="font-medium">Pró-labore fixo: {formatCurrency(fixed)}/mês</p>
          <p>
            {summary.fixedRemaining === 0
              ? 'Já pago integralmente este mês.'
              : summary.fixedCovered
                ? `Falta pagar ${formatCurrency(summary.fixedRemaining)} — o disponível cobre.`
                : `Falta pagar ${formatCurrency(summary.fixedRemaining)}, mas o disponível só cobre ${formatCurrency(summary.availableToWithdraw)}.`}
          </p>
        </div>
      )}

      {summary.cappedByBalance && (
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          O cálculo pelas receitas daria {formatCurrency(summary.calculatedAmount)}, mas o valor foi limitado ao que as
          contas PJ comportam hoje.
        </p>
      )}
      {summary.availableToWithdraw === 0 && (
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          Não sobra valor para retirar este mês com a configuração atual. Veja o detalhamento.
        </p>
      )}
      {!summary.hasPersonalAccounts && (
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          Cadastre uma conta pessoal (PF) para registrar o pagamento como transferência.
        </p>
      )}
      <Button variant="brand" className="mt-auto w-full sm:w-auto sm:self-start" onClick={onPay} disabled={!canPay}>
        {canPay ? `Pagar ${formatCurrency(summary.suggestedPayment)}` : 'Pagar pró-labore'}
      </Button>
    </Card>
  )
}

function BreakdownCard({ summary }: { summary: ProLaboreSummary }) {
  const { settings } = summary
  const isMonthIncome = settings.calculationBase === 'MONTH_INCOME'
  const taxSource = settings.taxMode === 'MANUAL' ? 'alíquota definida por você' : 'alíquota automática'

  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Como o valor é calculado</h3>
      <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
        Base: {isMonthIncome ? 'receitas do mês' : 'saldo atual das contas PJ'}
      </p>
      <dl className="mt-3 divide-y divide-zinc-200 text-sm dark:divide-zinc-700">
        {isMonthIncome ? (
          <>
            <BreakdownRow label="Receitas PJ recebidas no mês" value={summary.monthBusinessIncome} />
            <BreakdownRow
              label="Despesas PJ do mês"
              value={-summary.monthBusinessExpenses}
              detail="pagas no mês e pendentes até o fim dele"
            />
            <BreakdownRow
              label="Imposto"
              value={-summary.taxReserve}
              detail={`${formatPercent(summary.taxRate)} das receitas (${taxSource})`}
            />
            <BreakdownRow
              label="Reserva da empresa"
              value={-summary.reserve}
              detail={`${formatPercent(settings.reserveRate)} das receitas`}
            />
            {summary.cappedByBalance && summary.balanceCap !== null && (
              <BreakdownRow
                label="Limite pelo saldo PJ"
                value={summary.balanceCap}
                detail="saldo − contas a pagar − imposto + já retirado no mês"
              />
            )}
          </>
        ) : (
          <>
            <BreakdownRow label="Saldo nas contas PJ" value={summary.businessBalance} />
            <BreakdownRow
              label="Contas PJ a pagar até o fim do mês"
              value={-summary.pendingBusinessExpenses}
              detail="despesas pendentes, inclusive as atrasadas"
            />
            <BreakdownRow
              label="Imposto a reservar"
              value={-summary.taxReserve}
              detail={
                summary.taxReserveSaved > summary.taxOnMonthIncome
                  ? 'valor já guardado nas caixinhas do imposto'
                  : `${formatPercent(summary.taxRate)} de ${formatCurrency(summary.monthBusinessIncome)} recebidos no mês (${taxSource})`
              }
            />
            <BreakdownRow
              label="Colchão de caixa"
              value={-summary.cashCushion}
              detail={`${settings.cashCushionMonths} ${settings.cashCushionMonths === 1 ? 'mês' : 'meses'} × despesa média de ${formatCurrency(summary.averageMonthlyBusinessExpense)}`}
            />
            <BreakdownRow
              label="Pró-labore já retirado no mês"
              value={summary.withdrawnThisMonth}
              detail="já saiu do saldo, mas faz parte do pró-labore do mês"
            />
          </>
        )}
        <TotalRow label="Orçamento do pró-labore no mês" value={summary.monthBudget} />

        {(summary.withholdingApplied || settings.fixedAmount !== null) && (
          <BreakdownRow
            label={settings.fixedAmount !== null ? 'Pró-labore bruto (fixo)' : 'Pró-labore bruto'}
            value={summary.payroll.gross}
            detail={
              summary.payroll.employerInss > 0
                ? `o orçamento cobre o bruto + INSS patronal de ${formatPercent(summary.employerInssRate)}`
                : undefined
            }
          />
        )}
        {summary.withholdingApplied && (
          <>
            <BreakdownRow label="INSS do sócio" value={-summary.payroll.employeeInss} detail="11%, limitado ao teto" />
            <BreakdownRow label="IRRF" value={-summary.payroll.irrf} detail="tabela 2026, com o redutor até R$ 7.350" />
            {summary.payroll.employerInss > 0 && (
              <BreakdownRow
                label="INSS patronal (pago pela empresa)"
                value={summary.payroll.employerInss}
                detail="não sai do seu líquido"
                neutral
              />
            )}
            <TotalRow label="Pró-labore líquido" value={summary.payroll.net} />
          </>
        )}
        <BreakdownRow label="Já retirado no mês" value={-summary.withdrawnThisMonth} />
        <TotalRow
          label={settings.fixedAmount !== null ? 'Falta transferir do fixo' : 'Disponível para transferir'}
          value={summary.suggestedPayment}
        />
      </dl>
    </Card>
  )
}

function TotalRow({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between gap-3 py-2 font-semibold text-zinc-800 dark:text-zinc-100">
      <dt>{label}</dt>
      <dd>{formatCurrency(value)}</dd>
    </div>
  )
}

interface BreakdownRowProps {
  label: string
  value: number
  detail?: string
  /** Só informativo: não soma nem subtrai na conta (ex.: INSS patronal). */
  neutral?: boolean
}

function BreakdownRow({ label, value, detail, neutral = false }: BreakdownRowProps) {
  const color = neutral
    ? 'text-zinc-500 dark:text-zinc-400'
    : value < 0
      ? 'text-red-600 dark:text-red-400'
      : 'text-zinc-800 dark:text-zinc-100'
  return (
    <div className="flex items-start justify-between gap-3 py-2">
      <dt className="min-w-0">
        <span className="text-zinc-700 dark:text-zinc-200">{label}</span>
        {detail && <span className="block text-xs text-zinc-500 dark:text-zinc-400">{detail}</span>}
      </dt>
      <dd className={`shrink-0 ${color}`}>
        {value < 0 ? `− ${formatCurrency(-value)}` : formatCurrency(value)}
      </dd>
    </div>
  )
}

/**
 * Configuração do cálculo em largura total, recolhida por padrão. Fechar só esconde o formulário
 * (não desmonta), para não perder o que foi digitado e ainda não salvo.
 */
function SettingsCard({ summary }: { summary: ProLaboreSummary }) {
  const [open, setOpen] = useState(false)
  const { settings } = summary
  const taxLabel =
    settings.taxMode === 'MANUAL' && settings.manualTaxRate != null
      ? `imposto ${formatPercent(settings.manualTaxRate)}`
      : 'imposto automático'

  return (
    <Card>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-controls="pro-labore-settings"
        className="flex w-full items-center justify-between gap-3 text-left"
      >
        <span className="min-w-0">
          <span className="block text-base font-semibold text-zinc-800 dark:text-zinc-100">Configuração do cálculo</span>
          {!open && (
            <span className="block truncate text-xs text-zinc-500 dark:text-zinc-400">
              {CALCULATION_BASE_LABELS[settings.calculationBase]} · {taxLabel}
              {settings.fixedAmount != null && ` · fixo ${formatCurrency(settings.fixedAmount)}`}
            </span>
          )}
        </span>
        <ChevronDownIcon
          className={`h-4 w-4 shrink-0 text-zinc-500 transition-transform dark:text-zinc-400 ${open ? 'rotate-180' : ''}`}
        />
      </button>
      <div id="pro-labore-settings" className={open ? 'mt-4' : 'hidden'}>
        {/* key: ao salvar, o formulário recomeça dos valores devolvidos pelo backend. */}
        <ProLaboreSettingsForm key={JSON.stringify(settings)} settings={settings} />
      </div>
    </Card>
  )
}

const EXPENSE_STATUS_BADGE_CLASSES = {
  paid: 'bg-zinc-200 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300',
  pending: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300',
  overdue: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
}

/**
 * Pagamentos feitos pelas contas PJ que entraram no cálculo como despesa do mês. Não são
 * retiradas: pró-labore é só a transferência de PJ para PF.
 */
function BusinessExpensesCard({ summary }: { summary: ProLaboreSummary }) {
  return (
    <Card>
      <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Despesas PJ do mês</h3>
        {summary.businessExpenses.length > 0 && (
          <span className="text-sm font-semibold text-[#f06464]">{formatCurrency(summary.monthBusinessExpenses)}</span>
        )}
      </div>
      <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
        Pagamentos das contas PJ que já entram no cálculo: pagos no mês e pendentes até o fim dele. Não contam como
        retirada.
      </p>
      {summary.businessExpenses.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500 dark:text-zinc-400">Nenhuma despesa PJ este mês.</p>
      ) : (
        <ul className="mt-3 max-h-80 divide-y divide-zinc-200 overflow-y-auto pr-1 dark:divide-zinc-700">
          {summary.businessExpenses.map((expense) => {
            const status = expense.paid ? 'paid' : expense.overdue ? 'overdue' : 'pending'
            const statusLabel = expense.paid ? 'Paga' : expense.overdue ? 'Atrasada' : 'A pagar'
            const details = [formatDateOnlyBr(expense.date), expense.accountName, expense.categoryName]
              .filter(Boolean)
              .join(' · ')
            return (
              <li key={expense.transactionId} className="flex items-start justify-between gap-3 py-2 text-sm">
                <div className="min-w-0">
                  <p className="break-words text-zinc-800 dark:text-zinc-100">{expense.description}</p>
                  <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400">
                    <span className={`rounded-full px-2 py-0.5 font-medium ${EXPENSE_STATUS_BADGE_CLASSES[status]}`}>
                      {statusLabel}
                    </span>
                    <span className="min-w-0 break-words">{details}</span>
                  </div>
                </div>
                <span className="shrink-0 font-medium text-zinc-800 dark:text-zinc-100">
                  {formatCurrency(expense.amount)}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}

function WithdrawalsCard({ summary }: { summary: ProLaboreSummary }) {
  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Retiradas deste mês</h3>
      <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">Transferências de contas PJ para contas PF.</p>
      {summary.withdrawals.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500 dark:text-zinc-400">Nenhuma retirada este mês.</p>
      ) : (
        <ul className="mt-3 divide-y divide-zinc-200 dark:divide-zinc-700">
          {summary.withdrawals.map((withdrawal) => (
            <li key={withdrawal.transferId} className="flex items-center justify-between gap-3 py-2 text-sm">
              <div className="min-w-0">
                <p className="text-zinc-800 dark:text-zinc-100">{formatDateOnlyBr(withdrawal.date)}</p>
                <p className="truncate text-xs text-zinc-500 dark:text-zinc-400">
                  {withdrawal.fromAccountName} → {withdrawal.toAccountName}
                </p>
              </div>
              <span className="shrink-0 font-medium text-zinc-800 dark:text-zinc-100">
                {formatCurrency(withdrawal.amount)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
