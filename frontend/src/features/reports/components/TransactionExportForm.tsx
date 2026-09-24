import { useState, type FormEvent } from 'react'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useDownloadTransactionExport } from '../hooks/useDownloadTransactionExport'

export function TransactionExportForm() {
  const downloadExport = useDownloadTransactionExport()
  const { showToast } = useToast()
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const fileName = `transacoes-${referenceMonth}.csv`

    try {
      await downloadExport.mutateAsync({ referenceMonth, fileName })
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
          Gera um arquivo CSV com todas as transações do mês (em todas as contas, incluindo transferências), pronto
          para abrir no Excel ou Google Sheets.
        </p>
        <Button type="submit" disabled={downloadExport.isPending} className="w-full">
          {downloadExport.isPending ? 'Gerando...' : 'Exportar transações em CSV'}
        </Button>
      </div>
    </form>
  )
}
