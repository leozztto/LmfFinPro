import { formatCnpj, formatCpf, formatPhone } from '@/shared/format/mask'
import { CLIENT_WORK_TYPE_LABELS, type Client } from '../types'

interface ClientDetailsProps {
  client: Client
}

export function ClientDetails({ client }: ClientDetailsProps) {
  const document = client.documentNumber
    ? client.documentType === 'CNPJ'
      ? formatCnpj(client.documentNumber)
      : formatCpf(client.documentNumber)
    : null

  const rows: { label: string; value: string }[] = [
    { label: 'E-mail', value: client.email ?? '—' },
    { label: 'Telefone', value: client.phone ? formatPhone(client.phone) : '—' },
    { label: 'Tipo de trabalho', value: client.workType ? CLIENT_WORK_TYPE_LABELS[client.workType] : '—' },
    { label: 'Documento', value: document ? `${client.documentType} · ${document}` : '—' },
    { label: 'Status', value: client.active ? 'Ativo' : 'Inativo' },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <span
          className="h-3 w-3 shrink-0 rounded-full"
          style={{ backgroundColor: client.color ?? '#94a3b8' }}
          aria-hidden
        />
        <p className="break-words text-base font-semibold text-zinc-800 dark:text-zinc-100">{client.name}</p>
      </div>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
        {rows.map((row) => (
          <div key={row.label} className="contents">
            <dt className="text-zinc-500 dark:text-zinc-400">{row.label}</dt>
            <dd className="break-words text-zinc-800 dark:text-zinc-100">{row.value}</dd>
          </div>
        ))}
      </dl>
      {client.notes && (
        <div>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Observações</p>
          <p className="mt-1 whitespace-pre-wrap break-words text-sm text-zinc-800 dark:text-zinc-100">
            {client.notes}
          </p>
        </div>
      )}
    </div>
  )
}
