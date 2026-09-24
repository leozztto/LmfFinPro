import { useState, type FormEvent } from 'react'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useDownloadTransactionExport } from '../hooks/useDownloadTransactionExport'
import type { ReportFormat } from '../types'

export function TransactionExportForm({ format }: { format: ReportFormat }) {
  const downloadExport = useDownloadTransactionExport()
  const { showToast } = useToast()
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `transacoes-${referenceMonth}.${extension}`

    try {
      await downloadExport.mutateAsync({ referenceMonth, format, fileName })
      showToast('Exportação gerada com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar a exportação.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Mês de referência" htmlFor="transaction-export-month">
        <Input
          id="transaction-export-month"
          type="month"
          value={referenceMonth}
          onChange={(e) => setReferenceMonth(e.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um extrato bruto com todas as transações do mês (em todas as contas, incluindo transferências).
        </p>
        <Button type="submit" disabled={downloadExport.isPending} className="w-full">
          {downloadExport.isPending ? 'Gerando...' : `Exportar transações em ${format}`}
        </Button>
      </div>
    </form>
  )
}
