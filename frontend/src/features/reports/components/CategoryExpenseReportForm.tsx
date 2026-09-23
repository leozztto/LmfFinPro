import { useState, type FormEvent } from 'react'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useDownloadCategoryExpenseReport } from '../hooks/useDownloadCategoryExpenseReport'

export function CategoryExpenseReportForm() {
  const downloadReport = useDownloadCategoryExpenseReport()
  const { showToast } = useToast()
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const fileName = `despesas-por-categoria-${referenceMonth}.pdf`

    try {
      await downloadReport.mutateAsync({ referenceMonth, fileName })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Mês de referência" htmlFor="category-expense-month">
        <Input
          id="category-expense-month"
          type="month"
          value={referenceMonth}
          onChange={(e) => setReferenceMonth(e.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF com o total gasto em cada categoria no mês selecionado (somando todas as contas), útil
          para organizar despesas dedutíveis.
        </p>
        <Button type="submit" disabled={downloadReport.isPending} className="w-full">
          {downloadReport.isPending ? 'Gerando...' : 'Gerar relatório em PDF'}
        </Button>
      </div>
    </form>
  )
}
