import type { ReactNode } from 'react'
import type { OnboardingStepId } from '../types'

const highlightClassName =
  'ring-2 ring-[#1ea883] ring-offset-2 ring-offset-white animate-pulse dark:ring-[#2ad6a5] dark:ring-offset-zinc-900'

function MockField({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div>
      <p className="mb-1 text-xs font-medium text-zinc-600 dark:text-zinc-300">{label}</p>
      <div
        className={`rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-800 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100 ${highlight ? highlightClassName : ''}`}
      >
        {value}
      </div>
    </div>
  )
}

function MockButton({
  label,
  highlight = false,
  compact = false,
}: {
  label: string
  highlight?: boolean
  compact?: boolean
}) {
  return (
    <div
      className={`inline-flex items-center justify-center rounded-lg bg-primary-600 text-sm font-medium text-white ${compact ? 'h-9 w-9' : 'w-full px-4 py-2 sm:w-auto'} ${highlight ? highlightClassName : 'opacity-60'}`}
    >
      {label}
    </div>
  )
}

function MockFrame({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-xl border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
      <p className="mb-3 text-sm font-semibold text-zinc-800 dark:text-zinc-100">{title}</p>
      <div className="space-y-3">{children}</div>
    </div>
  )
}

function MockStat({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`rounded-lg bg-zinc-100 px-3 py-2 dark:bg-zinc-800 ${highlight ? highlightClassName : ''}`}>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">{label}</p>
      <p className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">{value}</p>
    </div>
  )
}

function AccountFields({ highlight }: { highlight: boolean }) {
  return (
    <>
      <MockField label="Nome" value="Conta Corrente" highlight={highlight} />
      <div className="grid gap-3 sm:grid-cols-2">
        <MockField label="Tipo" value="Conta corrente" highlight={highlight} />
        <MockField label="Saldo inicial" value="R$ 1.500,00" highlight={highlight} />
      </div>
    </>
  )
}

function BudgetFields({ highlight }: { highlight: boolean }) {
  return (
    <>
      <MockField label="Categoria" value="Alimentação" highlight={highlight} />
      <div className="grid gap-3 sm:grid-cols-2">
        <MockField label="Mês de referência" value="Outubro de 2026" highlight={highlight} />
        <MockField label="Valor limite" value="R$ 800,00" highlight={highlight} />
      </div>
    </>
  )
}

function TransactionFields({ highlight }: { highlight: boolean }) {
  return (
    <>
      <MockField label="Descrição" value="Supermercado" highlight={highlight} />
      <div className="grid gap-3 sm:grid-cols-3">
        <MockField label="Valor" value="R$ 182,40" highlight={highlight} />
        <MockField label="Data" value="05/10/2026" highlight={highlight} />
        <MockField label="Categoria" value="Alimentação" highlight={highlight} />
      </div>
    </>
  )
}

function RecurringFields({ highlight }: { highlight: boolean }) {
  return (
    <>
      <MockField label="Descrição" value="Aluguel" highlight={highlight} />
      <div className="grid gap-3 sm:grid-cols-3">
        <MockField label="Valor" value="R$ 1.200,00" highlight={highlight} />
        <MockField label="Frequência" value="Mensal" highlight={highlight} />
        <MockField label="Próxima data" value="10/11/2026" highlight={highlight} />
      </div>
    </>
  )
}

const CALENDAR_DAYS = Array.from({ length: 14 }, (_, index) => index + 1)

function MockCalendar() {
  return (
    <div className="grid grid-cols-7 gap-1.5 text-center text-xs">
      {CALENDAR_DAYS.map((day) => {
        const selected = day === 10
        const hasEntries = day === 5 || day === 10
        return (
          <div
            key={day}
            className={`rounded-lg bg-zinc-100 px-1 py-2 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200 ${selected ? highlightClassName : ''}`}
          >
            <p>{day}</p>
            <p className="h-3 text-[10px] leading-3 text-[#1ea883]">{hasEntries ? '• • •' : ''}</p>
          </div>
        )
      })}
    </div>
  )
}

const REPORT_TYPES = ['Fluxo de caixa', 'Gastos por categoria', 'Anexos e comprovantes']

/**
 * Ilustração do passo: uma réplica sem função da tela, com o ponto a usar em destaque. Nada aqui grava
 * dado; serve só para a pessoa ver onde clicar e o que preencher antes de fazer no sistema.
 */
export function TourPreview({ step }: { step: OnboardingStepId }) {
  return (
    <div aria-hidden="true" data-testid={`tour-preview-${step}`} className="pointer-events-none select-none">
      {step === 'ACCOUNT_OPEN' && (
        <MockFrame title="Contas">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma conta ainda.</p>
            <MockButton label="+" highlight compact />
          </div>
        </MockFrame>
      )}
      {step === 'ACCOUNT_FILL' && (
        <MockFrame title="Nova conta">
          <AccountFields highlight />
          <MockButton label="Adicionar conta" />
        </MockFrame>
      )}
      {step === 'ACCOUNT_SAVE' && (
        <MockFrame title="Nova conta">
          <AccountFields highlight={false} />
          <MockButton label="Adicionar conta" highlight />
        </MockFrame>
      )}
      {step === 'TRANSACTION_FILL' && (
        <MockFrame title="Nova transação">
          <TransactionFields highlight />
          <MockButton label="Salvar" />
        </MockFrame>
      )}
      {step === 'TRANSACTION_SAVE' && (
        <MockFrame title="Nova transação">
          <TransactionFields highlight={false} />
          <MockButton label="Salvar" highlight />
        </MockFrame>
      )}
      {step === 'IMPORT_PICK' && (
        <MockFrame title="Nova importação">
          <MockField label="Conta" value="Conta Corrente" highlight />
          <MockField label="Arquivo (CSV ou OFX)" value="extrato-outubro.csv" highlight />
          <MockButton label="Importar" highlight />
        </MockFrame>
      )}
      {step === 'IMPORT_RESULT' && (
        <MockFrame title="Resultado da importação">
          <div className="grid gap-3 sm:grid-cols-3">
            <MockStat label="Entrou" value="R$ 4.200,00" highlight />
            <MockStat label="Saiu" value="R$ 1.850,40" highlight />
            <MockStat label="Saldo do período" value="R$ 2.349,60" highlight />
          </div>
          <MockField label="Onde mais saiu" value="Moradia 64,9% · Alimentação 22,3% · Transporte 8,1%" />
        </MockFrame>
      )}
      {step === 'BUDGET_FILL' && (
        <MockFrame title="Novo orçamento">
          <BudgetFields highlight />
          <MockButton label="Salvar" />
        </MockFrame>
      )}
      {step === 'BUDGET_SAVE' && (
        <MockFrame title="Novo orçamento">
          <BudgetFields highlight={false} />
          <MockButton label="Salvar" highlight />
        </MockFrame>
      )}
      {step === 'RECURRING_FILL' && (
        <MockFrame title="Nova recorrência">
          <RecurringFields highlight />
          <MockButton label="Salvar" />
        </MockFrame>
      )}
      {step === 'RECURRING_SAVE' && (
        <MockFrame title="Nova recorrência">
          <RecurringFields highlight={false} />
          <MockButton label="Salvar" highlight />
        </MockFrame>
      )}
      {step === 'CALENDAR_VIEW' && (
        <MockFrame title="Outubro de 2026">
          <MockCalendar />
          <MockField label="Dia 10" value="Aluguel · R$ 1.200,00 a pagar" />
        </MockFrame>
      )}
      {step === 'DASHBOARD_VIEW' && (
        <MockFrame title="Dashboard">
          <div className="grid gap-3 sm:grid-cols-3">
            <MockStat label="Saldo" value="R$ 2.349,60" highlight />
            <MockStat label="Entradas" value="R$ 4.200,00" highlight />
            <MockStat label="Saídas" value="R$ 1.850,40" highlight />
          </div>
          <MockField label="Gastos por categoria" value="Moradia 64,9% · Alimentação 22,3%" highlight />
        </MockFrame>
      )}
      {step === 'REPORTS_VIEW' && (
        <MockFrame title="Relatórios">
          <MockField label="Período" value="01/10/2026 a 31/10/2026" highlight />
          {REPORT_TYPES.map((type) => (
            <MockField key={type} label="Relatório" value={type} />
          ))}
          <MockButton label="Exportar" highlight />
        </MockFrame>
      )}
    </div>
  )
}
