import { useState, type FormEvent } from 'react'
import { Button, FormField, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useDownloadIncomeStatement } from '../hooks/useDownloadIncomeStatement'
import { REPORT_GRANULARITY_LABELS, type ReportFormat, type ReportGranularity } from '../types'

const CURRENT_YEAR = new Date().getFullYear()
const YEAR_OPTIONS = Array.from({ length: 6 }, (_, i) => String(CURRENT_YEAR - i))
const GRANULARITY_OPTIONS: ReportGranularity[] = ['MONTHLY', 'QUARTERLY', 'YEARLY']

export function IncomeStatementForm({ format }: { format: ReportFormat }) {
  const downloadStatement = useDownloadIncomeStatement()
  const { showToast } = useToast()
  const [year, setYear] = useState(String(CURRENT_YEAR))
  const [granularity, setGranularity] = useState<ReportGranularity>('MONTHLY')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `resultado-periodo-${year}-${granularity}.${extension}`

    try {
      await downloadStatement.mutateAsync({ year, granularity, format, fileName })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Ano" htmlFor="income-statement-year">
        <Select id="income-statement-year" value={year} onChange={(e) => setYear(e.target.value)}>
          {YEAR_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Consolidação" htmlFor="income-statement-granularity">
        <Select
          id="income-statement-granularity"
          value={granularity}
          onChange={(e) => setGranularity(e.target.value as ReportGranularity)}
        >
          {GRANULARITY_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {REPORT_GRANULARITY_LABELS[option]}
            </option>
          ))}
        </Select>
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF com receita, despesa e resultado consolidados por mês, trimestre ou ano — um DRE simplificado do
          período selecionado.
        </p>
        <Button type="submit" disabled={downloadStatement.isPending} className="w-full">
          {downloadStatement.isPending ? 'Gerando...' : `Gerar resultado em ${format}`}
        </Button>
      </div>
    </form>
  )
}
