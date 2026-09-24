import { useState, type FormEvent } from 'react'
import { Button, FormField, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useClients } from '@/features/clients/hooks/useClients'
import { useDownloadClientAnnualStatement } from '../hooks/useDownloadClientAnnualStatement'
import type { ReportFormat } from '../types'

const CURRENT_YEAR = new Date().getFullYear()
const YEAR_OPTIONS = Array.from({ length: 6 }, (_, i) => String(CURRENT_YEAR - i))

export function ClientAnnualStatementForm({ format }: { format: ReportFormat }) {
  const { data: clients } = useClients()
  const downloadStatement = useDownloadClientAnnualStatement()
  const { showToast } = useToast()
  const [clientId, setClientId] = useState('')
  const [year, setYear] = useState(String(CURRENT_YEAR))

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!clientId) {
      showToast('Selecione um cliente.')
      return
    }

    const client = clients?.find((c) => c.id === Number(clientId))
    const clientSlug = (client?.name ?? 'cliente').toLowerCase().replace(/\s+/g, '-')
    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `demonstrativo-anual-${clientSlug}-${year}.${extension}`

    try {
      await downloadStatement.mutateAsync({ clientId: Number(clientId), year, format, fileName })
      showToast('Demonstrativo gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o demonstrativo.')
    }
  }

  if (!clients?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">Cadastre um cliente antes de gerar um demonstrativo.</p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Cliente" htmlFor="annual-statement-client">
        <Select
          id="annual-statement-client"
          value={clientId}
          onChange={(e) => setClientId(e.target.value)}
        >
          <option value="" disabled>
            Selecione...
          </option>
          {clients.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Ano" htmlFor="annual-statement-year">
        <Select id="annual-statement-year" value={year} onChange={(e) => setYear(e.target.value)}>
          {YEAR_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </Select>
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF com o total recebido do cliente em cada mês do ano selecionado, útil para declaração de IR ou
          para enviar ao próprio cliente.
        </p>
        <Button type="submit" disabled={downloadStatement.isPending} className="w-full">
          {downloadStatement.isPending ? 'Gerando...' : `Gerar demonstrativo em ${format}`}
        </Button>
      </div>
    </form>
  )
}
