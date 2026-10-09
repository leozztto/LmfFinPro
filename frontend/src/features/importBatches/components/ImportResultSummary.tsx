import { Link } from 'react-router-dom'
import { Card, StatCard } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { useImportSummary } from '../hooks/useImportSummary'

interface ImportResultSummaryProps {
  batchId: number
  /** Linhas do arquivo que já existiam e foram puladas. */
  duplicateCount?: number
}

/**
 * O primeiro retorno visível de um extrato: quanto entrou, quanto saiu, o saldo do período e onde
 * mais se gastou. Todos os números vêm calculados do servidor.
 */
export function ImportResultSummary({ batchId, duplicateCount = 0 }: ImportResultSummaryProps) {
  const { data, isLoading, isError } = useImportSummary(batchId)

  if (isLoading) return <p className="text-sm text-zinc-500 dark:text-zinc-400">Calculando o resultado…</p>
  if (isError || !data) {
    return (
      <p role="alert" className="text-sm text-red-600">
        A importação foi concluída, mas não conseguimos montar o resultado agora. Veja os lançamentos em Transações.
      </p>
    )
  }

  const period =
    data.firstDate && data.lastDate ? `${formatDateOnlyBr(data.firstDate)} a ${formatDateOnlyBr(data.lastDate)}` : null

  return (
    <section aria-label="Resultado da importação" className="space-y-4">
      <div>
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">
          {data.transactionCount} {data.transactionCount === 1 ? 'lançamento importado' : 'lançamentos importados'}
        </h3>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          {period && <>Período: {period}. </>}
          {duplicateCount > 0 && <>{duplicateCount} já existia(m) e foi(ram) ignorado(s).</>}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="Entrou" value={formatCurrency(data.totalIncome)} />
        <StatCard label="Saiu" value={formatCurrency(data.totalExpense)} />
        <StatCard label="Saldo do período" value={formatCurrency(data.balance)} />
      </div>

      {data.topExpenseCategories.length > 0 && (
        <Card padding="sm">
          <h4 className="mb-3 text-sm font-semibold text-zinc-800 dark:text-zinc-100">Onde mais saiu dinheiro</h4>
          <ul className="space-y-3">
            {data.topExpenseCategories.map((category) => (
              <li key={category.categoryId ?? 'sem-categoria'}>
                <div className="flex items-baseline justify-between gap-3 text-sm">
                  <span className="min-w-0 truncate text-zinc-700 dark:text-zinc-300">{category.name}</span>
                  <span className="shrink-0 text-zinc-800 dark:text-zinc-100">
                    {formatCurrency(category.total)}{' '}
                    <span className="text-xs text-zinc-500 dark:text-zinc-400">({category.share.toFixed(1)}%)</span>
                  </span>
                </div>
                <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-700">
                  <div
                    className="h-full rounded-full bg-[#1ea883] dark:bg-[#2ad6a5]"
                    style={{
                      width: `${Math.min(100, Math.max(2, category.share))}%`,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {data.uncategorizedCount > 0 && (
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          {data.uncategorizedCount} {data.uncategorizedCount === 1 ? 'lançamento ficou' : 'lançamentos ficaram'} sem
          categoria. Categorize na aba de importações: o sistema aprende e acerta nas próximas.{' '}
          <Link to="/importacoes" className={authLinkClassName}>
            Revisar agora
          </Link>
        </p>
      )}
    </section>
  )
}
