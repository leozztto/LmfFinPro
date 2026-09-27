import { useState, type FormEvent } from 'react'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentIsoDate } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { ACCOUNT_SCOPE_LABELS } from '@/features/accounts/types'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useTags } from '@/features/tags/hooks/useTags'
import { TagInput } from '@/features/tags/components/TagInput'
import { useDownloadTransactionReport } from '../hooks/useDownloadTransactionReport'
import {
  EMPTY_TRANSACTION_REPORT_FILTERS,
  type ReportFormat,
  type TransactionReportFilters,
  type TransactionReportKind,
} from '../types'
import { validateTransactionReportFilters } from '../utils'

interface TransactionReportFormProps {
  kind: TransactionReportKind
  format: ReportFormat
}

/** Relatório de receitas ou de despesas: todos os filtros são opcionais. */
export function TransactionReportForm({ kind, format }: TransactionReportFormProps) {
  const downloadReport = useDownloadTransactionReport()
  const { showToast } = useToast()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const { data: tags } = useTags()
  const [filters, setFilters] = useState<TransactionReportFilters>(EMPTY_TRANSACTION_REPORT_FILTERS)
  const [tagNames, setTagNames] = useState<string[]>([])

  const isIncome = kind === 'INCOME'
  const noun = isIncome ? 'receitas' : 'despesas'
  const validationError = validateTransactionReportFilters(filters)
  const hasFilters = Object.values(filters).some((value) => value.trim() !== '') || tagNames.length > 0
  const categoriesOfKind = (categories ?? []).filter((category) => category.type === kind)
  const idPrefix = `${noun}-report`

  function update<K extends keyof TransactionReportFilters>(key: K, value: string) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (validationError) return
    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `${noun}-${getCurrentIsoDate()}.${extension}`

    try {
      const tagIds = (tags ?? []).filter((tag) => tagNames.includes(tag.name)).map((tag) => tag.id)
      await downloadReport.mutateAsync({ kind, filters, format, fileName, tagIds })
      showToast('Relatório gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o relatório.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Todos os filtros são opcionais — deixe em branco o que não quiser filtrar. Transferências entre suas contas não
        entram no relatório.
      </p>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <FormField label="De" htmlFor={`${idPrefix}-start`}>
          <Input
            id={`${idPrefix}-start`}
            type="date"
            value={filters.startDate}
            onChange={(e) => update('startDate', e.target.value)}
          />
        </FormField>
        <FormField label="Até" htmlFor={`${idPrefix}-end`}>
          <Input
            id={`${idPrefix}-end`}
            type="date"
            value={filters.endDate}
            onChange={(e) => update('endDate', e.target.value)}
          />
        </FormField>
        <FormField label="Situação" htmlFor={`${idPrefix}-status`}>
          <Select id={`${idPrefix}-status`} value={filters.status} onChange={(e) => update('status', e.target.value)}>
            <option value="">Todas</option>
            <option value="PAID">{isIncome ? 'Recebidas' : 'Pagas'}</option>
            <option value="PENDING">{isIncome ? 'A receber' : 'A pagar'}</option>
          </Select>
        </FormField>
        <FormField label="Conta" htmlFor={`${idPrefix}-account`}>
          <Select
            id={`${idPrefix}-account`}
            value={filters.accountId}
            onChange={(e) => update('accountId', e.target.value)}
          >
            <option value="">Todas</option>
            {(accounts ?? []).map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Uso da conta" htmlFor={`${idPrefix}-scope`}>
          <Select
            id={`${idPrefix}-scope`}
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
        <FormField label="Categoria" htmlFor={`${idPrefix}-category`}>
          <Select
            id={`${idPrefix}-category`}
            value={filters.categoryId}
            onChange={(e) => update('categoryId', e.target.value)}
          >
            <option value="">Todas</option>
            {categoriesOfKind.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Cliente" htmlFor={`${idPrefix}-client`}>
          <Select id={`${idPrefix}-client`} value={filters.clientId} onChange={(e) => update('clientId', e.target.value)}>
            <option value="">Todos</option>
            {(clients ?? []).map((client) => (
              <option key={client.id} value={client.id}>
                {client.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Valor mínimo" htmlFor={`${idPrefix}-min`}>
          <Input
            id={`${idPrefix}-min`}
            type="number"
            step="0.01"
            min={0}
            inputMode="decimal"
            value={filters.minAmount}
            onChange={(e) => update('minAmount', e.target.value)}
          />
        </FormField>
        <FormField label="Valor máximo" htmlFor={`${idPrefix}-max`}>
          <Input
            id={`${idPrefix}-max`}
            type="number"
            step="0.01"
            min={0}
            inputMode="decimal"
            value={filters.maxAmount}
            onChange={(e) => update('maxAmount', e.target.value)}
          />
        </FormField>
        <div className="min-w-0 sm:col-span-2 lg:col-span-3">
          <FormField label="Tags (qualquer uma)" htmlFor={`${idPrefix}-tags`}>
            <TagInput
              id={`${idPrefix}-tags`}
              value={tagNames}
              onChange={setTagNames}
              allowCreate={false}
              placeholder="Todas"
            />
          </FormField>
        </div>
        <div className="sm:col-span-2 lg:col-span-3">
          <FormField label="Descrição contém" htmlFor={`${idPrefix}-description`}>
            <Input
              id={`${idPrefix}-description`}
              placeholder="Ex.: aluguel"
              value={filters.description}
              onChange={(e) => update('description', e.target.value)}
            />
          </FormField>
        </div>
      </div>

      {validationError && <p className="text-sm text-red-600">{validationError}</p>}

      <div className="flex flex-col gap-2 sm:flex-row">
        <Button
          type="button"
          variant="secondary"
          className="w-full sm:w-auto"
          onClick={() => {
            setFilters(EMPTY_TRANSACTION_REPORT_FILTERS)
            setTagNames([])
          }}
          disabled={!hasFilters}
        >
          Limpar filtros
        </Button>
        <Button
          type="submit"
          disabled={downloadReport.isPending || validationError !== null}
          className="w-full sm:flex-1"
        >
          {downloadReport.isPending ? 'Gerando...' : `Gerar relatório de ${noun} em ${format}`}
        </Button>
      </div>
    </form>
  )
}
