import { useState, type FormEvent } from 'react'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useClients } from '@/features/clients/hooks/useClients'
import { useDownloadClientReceipt } from '../hooks/useDownloadClientReceipt'

export function ClientReceiptForm() {
  const { data: clients } = useClients()
  const downloadReceipt = useDownloadClientReceipt()
  const { showToast } = useToast()
  const [clientId, setClientId] = useState('')
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!clientId) {
      showToast('Selecione um cliente.')
      return
    }

    const client = clients?.find((c) => c.id === Number(clientId))
    const clientSlug = (client?.name ?? 'cliente').toLowerCase().replace(/\s+/g, '-')
    const fileName = `recibo-${clientSlug}-${referenceMonth}.pdf`

    try {
      await downloadReceipt.mutateAsync({ clientId: Number(clientId), referenceMonth, fileName })
      showToast('Recibo gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o recibo.')
    }
  }

  if (!clients?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">Cadastre um cliente antes de gerar um recibo.</p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Cliente" htmlFor="receipt-client">
        <Select id="receipt-client" value={clientId} onChange={(e) => setClientId(e.target.value)}>
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
      <FormField label="Mês de referência" htmlFor="receipt-month">
        <Input
          id="receipt-month"
          type="month"
          value={referenceMonth}
          onChange={(e) => setReferenceMonth(e.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF com as receitas lançadas para o cliente no mês selecionado, com dados de emissor e cliente.
        </p>
        <Button type="submit" disabled={downloadReceipt.isPending} className="w-full">
          {downloadReceipt.isPending ? 'Gerando...' : 'Gerar recibo em PDF'}
        </Button>
      </div>
    </form>
  )
}
