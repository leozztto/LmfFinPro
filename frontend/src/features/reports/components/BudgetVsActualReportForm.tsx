import { useState, type FormEvent } from 'react'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useDownloadBudgetVsActualReport } from '../hooks/useDownloadBudgetVsActualReport'

export function BudgetVsActualReportForm() {
  const downloadReport = useDownloadBudgetVsActualReport()
  const { showToast } = useToast()
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const fileName = `orcamento-vs-realizado-${referenceMonth}.pdf`

    try {
      await downloadReport.mutateAsync({ referenceMonth, fileName })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Mês de referência" htmlFor="budget-vs-actual-month">
        <Input
          id="budget-vs-actual-month"
          type="month"
          value={referenceMonth}
          onChange={(e) => setReferenceMonth(e.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF comparando, para cada orçamento cadastrado no mês, o limite definido com o gasto real na
          categoria — destacando em vermelho os que estouraram o limite.
        </p>
        <Button type="submit" disabled={downloadReport.isPending} className="w-full">
          {downloadReport.isPending ? 'Gerando...' : 'Gerar relatório em PDF'}
        </Button>
      </div>
    </form>
  )
}
