import { useState, type FormEvent } from 'react'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentYearMonth } from '@/shared/format/date'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useDownloadAccountStatement } from '../hooks/useDownloadAccountStatement'
import type { ReportFormat } from '../types'

export function AccountStatementForm({ format }: { format: ReportFormat }) {
  const { data: accounts } = useAccounts()
  const downloadStatement = useDownloadAccountStatement()
  const { showToast } = useToast()
  const [accountId, setAccountId] = useState('')
  const [referenceMonth, setReferenceMonth] = useState(getCurrentYearMonth())

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!accountId) {
      showToast('Selecione uma conta.')
      return
    }

    const account = accounts?.find((a) => a.id === Number(accountId))
    const accountSlug = (account?.name ?? 'conta').toLowerCase().replace(/\s+/g, '-')
    const extension = format === 'CSV' ? 'csv' : 'pdf'
    const fileName = `extrato-${accountSlug}-${referenceMonth}.${extension}`

    try {
      await downloadStatement.mutateAsync({
        accountId: Number(accountId),
        referenceMonth,
        format,
        fileName,
      })
      showToast('Extrato gerado com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o extrato.')
    }
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">Cadastre uma conta antes de gerar um extrato.</p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Conta" htmlFor="statement-account">
        <Select id="statement-account" value={accountId} onChange={(e) => setAccountId(e.target.value)}>
          <option value="" disabled>
            Selecione...
          </option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Mês de referência" htmlFor="statement-month">
        <Input
          id="statement-month"
          type="month"
          value={referenceMonth}
          onChange={(e) => setReferenceMonth(e.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Gera um PDF com o saldo de abertura, todas as movimentações (receitas e despesas) da conta no mês
          selecionado e o saldo final do período.
        </p>
        <Button type="submit" disabled={downloadStatement.isPending} className="w-full">
          {downloadStatement.isPending ? 'Gerando...' : `Gerar extrato em ${format}`}
        </Button>
      </div>
    </form>
  )
}
