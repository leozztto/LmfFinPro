import { useState, type FormEvent } from 'react'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentIsoDate } from '@/shared/format/date'
import { ACCOUNT_SCOPE_LABELS } from '@/features/accounts/types'
import { useTags } from '@/features/tags/hooks/useTags'
import { TagInput } from '@/features/tags/components/TagInput'
import { useDownloadTagTotalsReport } from '../hooks/useDownloadTagTotalsReport'
import { EMPTY_TAG_TOTALS_REPORT_FILTERS, type ReportFormat, type TagTotalsReportFilters } from '../types'

interface TagTotalsReportFormProps {
  format: ReportFormat
}

/** Receita, despesa e resultado de cada tag no período — ex.: quanto um projeto deu de lucro. */
export function TagTotalsReportForm({ format }: TagTotalsReportFormProps) {
  const downloadReport = useDownloadTagTotalsReport()
  const { showToast } = useToast()
  const { data: tags } = useTags()
  const [filters, setFilters] = useState<TagTotalsReportFilters>(EMPTY_TAG_TOTALS_REPORT_FILTERS)
  const [tagNames, setTagNames] = useState<string[]>([])

  const periodError =
    filters.startDate && filters.endDate && filters.startDate > filters.endDate
      ? 'A data inicial deve ser anterior ou igual à data final.'
      : null

  function update<K extends keyof TagTotalsReportFilters>(key: K, value: string) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (periodError) return
    const tagIds = (tags ?? []).filter((tag) => tagNames.includes(tag.name)).map((tag) => tag.id)
    const fileName = `totais-por-tag-${getCurrentIsoDate()}.${format === 'CSV' ? 'csv' : 'pdf'}`
    try {
      await downloadReport.mutateAsync({ filters, tagIds, format, fileName })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  if (tags && tags.length === 0) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Você ainda não tem tags. Adicione tags às transações (ou crie em Categorias e tags) para ver os totais por tag.
      </p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Todos os filtros são opcionais. Sem escolher tags, entram todas, mais uma linha com o que ficou sem tag. Uma
        transação com duas tags conta nas duas.
      </p>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <FormField label="De" htmlFor="tag-totals-start">
          <Input
            id="tag-totals-start"
            type="date"
            value={filters.startDate}
            onChange={(e) => update('startDate', e.target.value)}
          />
        </FormField>
        <FormField label="Até" htmlFor="tag-totals-end">
          <Input
            id="tag-totals-end"
            type="date"
            value={filters.endDate}
            onChange={(e) => update('endDate', e.target.value)}
          />
        </FormField>
        <FormField label="Situação" htmlFor="tag-totals-status">
          <Select id="tag-totals-status" value={filters.status} onChange={(e) => update('status', e.target.value)}>
            <option value="">Pagas e pendentes</option>
            <option value="PAID">Só pagas/recebidas</option>
            <option value="PENDING">Só pendentes</option>
          </Select>
        </FormField>
        <FormField label="Uso da conta" htmlFor="tag-totals-scope">
          <Select
            id="tag-totals-scope"
            value={filters.accountScope}
            onChange={(e) => update('accountScope', e.target.value)}
          >
            <option value="">Pessoal e empresa</option>
            {Object.entries(ACCOUNT_SCOPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
        <div className="min-w-0 sm:col-span-2 lg:col-span-4">
          <FormField label="Tags" htmlFor="tag-totals-tags">
            <TagInput
              id="tag-totals-tags"
              value={tagNames}
              onChange={setTagNames}
              allowCreate={false}
              placeholder="Todas"
            />
          </FormField>
        </div>
      </div>

      {periodError && <p className="text-sm text-red-600">{periodError}</p>}

      <Button type="submit" disabled={downloadReport.isPending || periodError !== null} className="w-full">
        {downloadReport.isPending ? 'Gerando...' : `Gerar totais por tag em ${format}`}
      </Button>
    </form>
  )
}
