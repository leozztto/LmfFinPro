import { useMemo, useRef, useState, type Ref } from 'react'
import { Card, FormField, Select, StatCard } from '@/shared/ui'
import { AlertTriangleIcon, CheckCircleIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useTheme } from '@/shared/theme/ThemeContext'
import { CATEGORICAL_PALETTE_DARK, CATEGORICAL_PALETTE_LIGHT, NEUTRAL_SERIES_COLOR } from '@/shared/chart/palette'
import { MonthlyFlowChart } from '@/features/dashboard/components/MonthlyFlowChart'
import { formatMonthLabel } from '@/features/dashboard/utils'
import { useClientAnalytics } from '../hooks/useClientAnalytics'
import {
  STACKED_TOP_CLIENTS,
  assignClientColors,
  buildStackedHistory,
  describeRisk,
  formatShare,
  type RiskInfo,
} from '../analytics'
import type { ClientAnalytics, ClientRankingRow } from '../types'
import { ClientRevenueHistoryChart } from './ClientRevenueHistoryChart'

const PERIOD_OPTIONS = [3, 6, 12, 24]

export function ClientAnalyticsPanel() {
  const [months, setMonths] = useState(12)
  const [onlyReceived, setOnlyReceived] = useState(false)
  const [selectedClientId, setSelectedClientId] = useState<number | null>(null)
  const { data, isLoading, isError } = useClientAnalytics(months, onlyReceived)

  return (
    <div className="space-y-6">
      {/* Filtros numa linha só no tablet/desktop; empilhados no celular. */}
      <div className="grid gap-3 sm:grid-cols-2 lg:max-w-xl">
        <FormField label="Período" htmlFor="client-analytics-period">
          <Select
            id="client-analytics-period"
            value={months}
            onChange={(event) => setMonths(Number(event.target.value))}
          >
            {PERIOD_OPTIONS.map((option) => (
              <option key={option} value={option}>
                Últimos {option} meses
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Receitas" htmlFor="client-analytics-status">
          <Select
            id="client-analytics-status"
            value={onlyReceived ? 'received' : 'all'}
            onChange={(event) => setOnlyReceived(event.target.value === 'received')}
          >
            <option value="all">Todas (recebidas e a receber)</option>
            <option value="received">Só as já recebidas</option>
          </Select>
        </FormField>
      </div>

      {isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando análise...</p>}
      {isError && <p className="text-sm text-red-600">Não foi possível carregar a análise de clientes.</p>}
      {data && (
        <AnalyticsContent data={data} selectedClientId={selectedClientId} onSelectClient={setSelectedClientId} />
      )}
    </div>
  )
}

interface AnalyticsContentProps {
  data: ClientAnalytics
  selectedClientId: number | null
  onSelectClient: (clientId: number) => void
}

function AnalyticsContent({ data, selectedClientId, onSelectClient }: AnalyticsContentProps) {
  const historyRef = useRef<HTMLElement>(null)
  const { theme } = useTheme()
  const palette = theme === 'dark' ? CATEGORICAL_PALETTE_DARK : CATEGORICAL_PALETTE_LIGHT
  const colors = useMemo(() => assignClientColors(data.ranking, palette), [data.ranking, palette])
  const stacked = useMemo(() => buildStackedHistory(data.months, data.ranking, colors), [data, colors])
  const chartedIds = new Set(
    data.ranking
      .filter((row) => row.income > 0)
      .slice(0, STACKED_TOP_CLIENTS)
      .map((row) => row.clientId),
  )
  const colorFor = (row: ClientRankingRow) =>
    chartedIds.has(row.clientId) ? (colors.get(row.clientId) ?? NEUTRAL_SERIES_COLOR) : NEUTRAL_SERIES_COLOR

  const topClient = data.ranking.find((row) => row.income > 0)
  const unassignedShare = data.totalIncome > 0 ? data.unassignedIncome / data.totalIncome : 0
  const risk = describeRisk(data.risk, data.topClientShare, topClient?.name, unassignedShare)
  const selected = data.ranking.find((row) => row.clientId === selectedClientId) ?? data.ranking[0]

  /** Escolher no ranking leva até o histórico, que fica logo abaixo. */
  function selectFromRanking(clientId: number) {
    onSelectClient(clientId)
    historyRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <>
      <RiskCallout risk={risk} />

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard
          label="Receita no período"
          value={formatCurrency(data.totalIncome)}
          hint={
            data.unassignedIncome > 0 ? `${formatCurrency(data.unassignedIncome)} sem cliente vinculado` : undefined
          }
        />
        <StatCard label="Clientes com receita" value={String(data.activeClients)} />
        <StatCard label="Ticket médio" value={formatCurrency(data.averageTicket)} hint="por recebimento com cliente" />
        <StatCard
          label="Maior cliente"
          value={formatShare(data.topClientShare)}
          hint={`Top 3: ${formatShare(data.topThreeShare)} da receita`}
        />
      </div>

      <ClientRevenueHistoryChart data={stacked.data} series={stacked.series} />

      {data.ranking.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma receita ou despesa vinculada a clientes no período. Vincule o cliente nos lançamentos para ver o
          ranking.
        </p>
      ) : (
        <>
          <RankingCard
            ranking={data.ranking}
            selectedClientId={selected?.clientId}
            colorFor={colorFor}
            onSelectClient={selectFromRanking}
          />
          {selected && (
            <ClientHistorySection
              ref={historyRef}
              ranking={data.ranking}
              selected={selected}
              color={colorFor(selected)}
              onSelectClient={onSelectClient}
            />
          )}
        </>
      )}
    </>
  )
}

interface ClientHistorySectionProps {
  ranking: ClientRankingRow[]
  selected: ClientRankingRow
  color: string
  onSelectClient: (clientId: number) => void
  ref: Ref<HTMLElement>
}

/** Histórico mês a mês de um cliente, com seletor próprio para trocar de cliente sem voltar ao ranking. */
function ClientHistorySection({ ranking, selected, color, onSelectClient, ref }: ClientHistorySectionProps) {
  return (
    <section ref={ref} className="scroll-mt-4 space-y-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Histórico do cliente</h3>
        <div className="sm:w-72">
          <Select
            aria-label="Cliente do histórico"
            value={selected.clientId}
            onChange={(event) => onSelectClient(Number(event.target.value))}
          >
            {ranking.map((row) => (
              <option key={row.clientId} value={row.clientId}>
                {row.name}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="min-w-0 lg:col-span-2">
          <MonthlyFlowChart
            data={selected.monthly.map((month) => ({ ...month, label: formatMonthLabel(month.month) }))}
          />
        </div>
        <Card padding="sm" className="min-w-0">
          <p className="flex min-w-0 items-center gap-2">
            <span className="inline-block h-2.5 w-2.5 shrink-0 rounded-sm" style={{ backgroundColor: color }} aria-hidden />
            <span className="truncate font-semibold text-zinc-800 dark:text-zinc-100" title={selected.name}>
              {selected.name}
            </span>
          </p>
          {/* 2 colunas no celular/tablet (o card ocupa a largura toda); 1 coluna ao lado do gráfico. */}
          <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-3 text-sm lg:grid-cols-1">
            <SummaryItem label="Receita" value={formatCurrency(selected.income)} />
            <SummaryItem label="Participação" value={formatShare(selected.share)} />
            <SummaryItem label="Despesas vinculadas" value={formatCurrency(selected.expense)} />
            <SummaryItem
              label="Líquido"
              value={formatCurrency(selected.net)}
              negative={selected.net < 0}
            />
            <SummaryItem
              label="Recebimentos"
              value={`${selected.incomeCount} · ticket ${formatCurrency(selected.averageTicket)}`}
            />
            <SummaryItem
              label="Meses com receita"
              value={`${selected.activeMonths} de ${selected.monthly.length}`}
            />
            <SummaryItem
              label="Último recebimento"
              value={selected.lastIncomeDate ? formatDateOnlyBr(selected.lastIncomeDate) : '—'}
            />
          </dl>
        </Card>
      </div>
    </section>
  )
}

function SummaryItem({ label, value, negative = false }: { label: string; value: string; negative?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-zinc-500 dark:text-zinc-400">{label}</dt>
      <dd
        className={`truncate font-medium ${
          negative ? 'text-red-600 dark:text-red-400' : 'text-zinc-800 dark:text-zinc-100'
        }`}
      >
        {value}
      </dd>
    </div>
  )
}

const RISK_STYLES: Record<RiskInfo['tone'], string> = {
  critical: 'border-red-200 bg-red-50 text-red-800 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300',
  warning:
    'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300',
  good: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300',
  neutral: 'border-zinc-200 bg-zinc-50 text-zinc-700 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300',
}

/** Status sempre com ícone + rótulo, nunca só a cor. */
function RiskCallout({ risk }: { risk: RiskInfo }) {
  const Icon = risk.tone === 'good' ? CheckCircleIcon : AlertTriangleIcon
  return (
    <div role="status" className={`flex items-start gap-3 rounded-xl border p-4 ${RISK_STYLES[risk.tone]}`}>
      <Icon className="mt-0.5 h-5 w-5 shrink-0" />
      <div>
        <p className="font-semibold">{risk.label}</p>
        <p className="text-sm">{risk.description}</p>
      </div>
    </div>
  )
}

interface RankingCardProps {
  ranking: ClientRankingRow[]
  selectedClientId: number | undefined
  colorFor: (row: ClientRankingRow) => string
  onSelectClient: (clientId: number) => void
}

function RankingCard({ ranking, selectedClientId, colorFor, onSelectClient }: RankingCardProps) {
  return (
    <Card padding="sm">
      <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Ranking de clientes</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Escolha um cliente para ver o histórico mês a mês logo abaixo.
      </p>

      {/* Celular e tablet: um card por cliente (em 2 colunas no tablet). */}
      <ul className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:hidden">
        {ranking.map((row, index) => (
          <li key={row.clientId} className="min-w-0">
            <button
              type="button"
              onClick={() => onSelectClient(row.clientId)}
              className={`w-full rounded-lg border p-3 text-left ${
                row.clientId === selectedClientId
                  ? 'border-[#2ad6a5] bg-[#2ad6a5]/5'
                  : 'border-zinc-200 dark:border-zinc-700'
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <ClientName row={row} position={index + 1} color={colorFor(row)} />
                <span className="shrink-0 font-semibold text-zinc-800 dark:text-zinc-100">{formatCurrency(row.income)}</span>
              </div>
              <ShareBar share={row.share} color={colorFor(row)} />
              <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                {row.incomeCount} recebimento(s) · ticket {formatCurrency(row.averageTicket)} · líquido{' '}
                {formatCurrency(row.net)}
              </p>
            </button>
          </li>
        ))}
      </ul>

      {/* Desktop: tabela. Com a barra lateral, o conteúdo tem ~720px no desktop pequeno, então as colunas
          secundárias só aparecem em telas largas (e seguem no card "Histórico do cliente"). */}
      <div className="mt-3 hidden overflow-x-auto lg:block">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-zinc-200 text-left text-xs text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
              <th className="py-2 pr-3 font-medium">Cliente</th>
              <th className="py-2 pr-3 text-right font-medium">Receita</th>
              <th className="py-2 pr-3 font-medium">Participação</th>
              <th className="py-2 pr-3 text-right font-medium">Receb.</th>
              <th className="py-2 pr-3 text-right font-medium">Ticket médio</th>
              <th className="hidden py-2 pr-3 text-right font-medium xl:table-cell">Despesas</th>
              <th className="py-2 pr-3 text-right font-medium">Líquido</th>
              <th className="hidden py-2 text-right font-medium xl:table-cell">Último receb.</th>
            </tr>
          </thead>
          <tbody>
            {ranking.map((row, index) => (
              <tr
                key={row.clientId}
                onClick={() => onSelectClient(row.clientId)}
                className={`cursor-pointer border-b border-zinc-100 last:border-0 dark:border-zinc-700/60 ${
                  row.clientId === selectedClientId ? 'bg-[#2ad6a5]/5' : 'hover:bg-zinc-100/60 dark:hover:bg-zinc-700/30'
                }`}
              >
                <td className="max-w-[14rem] py-2 pr-3">
                  {/* O clique sobe para a linha; o botão existe para o teclado e leitores de tela. */}
                  <button type="button" className="w-full min-w-0 text-left" aria-pressed={row.clientId === selectedClientId}>
                    <ClientName row={row} position={index + 1} color={colorFor(row)} />
                  </button>
                </td>
                <td className="py-2 pr-3 text-right font-medium text-zinc-800 dark:text-zinc-100">
                  {formatCurrency(row.income)}
                </td>
                <td className="min-w-28 py-2 pr-3">
                  <ShareBar share={row.share} color={colorFor(row)} />
                </td>
                <td className="py-2 pr-3 text-right text-zinc-600 dark:text-zinc-300">{row.incomeCount}</td>
                <td className="py-2 pr-3 text-right text-zinc-600 dark:text-zinc-300">
                  {formatCurrency(row.averageTicket)}
                </td>
                <td className="hidden py-2 pr-3 text-right text-zinc-600 dark:text-zinc-300 xl:table-cell">
                  {formatCurrency(row.expense)}
                </td>
                <td
                  className={`py-2 pr-3 text-right font-medium ${
                    row.net < 0 ? 'text-red-600 dark:text-red-400' : 'text-zinc-800 dark:text-zinc-100'
                  }`}
                >
                  {formatCurrency(row.net)}
                </td>
                <td className="hidden py-2 text-right text-zinc-600 dark:text-zinc-300 xl:table-cell">
                  {row.lastIncomeDate ? formatDateOnlyBr(row.lastIncomeDate) : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

function ClientName({ row, position, color }: { row: ClientRankingRow; position: number; color: string }) {
  return (
    <span className="flex min-w-0 items-center gap-2">
      <span className="w-5 shrink-0 text-xs text-zinc-400">{position}º</span>
      <span className="inline-block h-2.5 w-2.5 shrink-0 rounded-sm" style={{ backgroundColor: color }} aria-hidden />
      <span className="truncate font-medium text-zinc-800 dark:text-zinc-100" title={row.name}>
        {row.name}
      </span>
    </span>
  )
}

function ShareBar({ share, color }: { share: number; color: string }) {
  return (
    <div className="mt-1 flex items-center gap-2">
      <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-700">
        <div className="h-full rounded-full" style={{ width: `${Math.min(100, share * 100)}%`, backgroundColor: color }} />
      </div>
      <span className="w-12 shrink-0 text-right text-xs text-zinc-600 dark:text-zinc-300">{formatShare(share)}</span>
    </div>
  )
}
