import { useState, type FormEvent } from 'react'
import { Button, FormField, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useDownloadNetWorthReport } from '../hooks/useDownloadNetWorthReport'
import type { ReportFormat } from '../types'

const PERIOD_OPTIONS = [6, 12, 24, 36] as const

export function NetWorthReportForm({ format }: { format: ReportFormat }) {
  const downloadReport = useDownloadNetWorthReport()
  const { showToast } = useToast()
  const [months, setMonths] = useState<number>(12)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `evolucao-patrimonial-${months}-meses.${extension}`

    try {
      await downloadReport.mutateAsync({ months, format, fileName })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Período" htmlFor="net-worth-months">
        <Select id="net-worth-months" value={months} onChange={(e) => setMonths(Number(e.target.value))}>
          {PERIOD_OPTIONS.map((option) => (
            <option key={option} value={option}>
              Últimos {option} meses
            </option>
          ))}
        </Select>
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um relatório com o patrimônio líquido mês a mês (contas + investimentos − dívidas) e a composição de
          hoje: saldo das contas, rendimento dos investimentos e saldo devedor das dívidas. Os valores são os mesmos da
          tela de Patrimônio.
        </p>
        <Button type="submit" disabled={downloadReport.isPending} className="w-full">
          {downloadReport.isPending ? 'Gerando...' : `Gerar relatório em ${format}`}
        </Button>
      </div>
    </form>
  )
}
