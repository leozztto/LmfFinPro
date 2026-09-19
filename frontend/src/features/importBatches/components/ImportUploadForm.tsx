import { useRef, useState, type FormEvent } from 'react'
import { Button, FileInput, FormField, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useUploadImportBatch } from '../hooks/useUploadImportBatch'
import type { ImportBatch } from '../types'

interface ImportUploadFormProps {
  onSuccess?: (batch: ImportBatch) => void
}

export function ImportUploadForm({ onSuccess }: ImportUploadFormProps) {
  const { data: accounts } = useAccounts()
  const uploadBatch = useUploadImportBatch()
  const { showToast } = useToast()
  const [accountId, setAccountId] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!accountId || !file) {
      showToast('Selecione a conta e o arquivo CSV.')
      return
    }

    try {
      const batch = await uploadBatch.mutateAsync({ accountId: Number(accountId), file })
      setFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
      showToast(
        `Importação concluída: ${batch.transactionCount} transações, ${batch.uncategorizedCount} sem categoria.`,
        'success',
      )
      onSuccess?.(batch)
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível importar o arquivo.')
    }
  }

  if (!accounts?.length) {
    return (
      <p className="text-sm text-zinc-500 dark:text-zinc-400">Cadastre uma conta antes de importar um extrato.</p>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Conta" htmlFor="import-account">
        <Select id="import-account" value={accountId} onChange={(e) => setAccountId(e.target.value)}>
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
      <FormField label="Arquivo CSV" htmlFor="import-file">
        <FileInput
          id="import-file"
          ref={fileInputRef}
          accept=".csv,text/csv"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="text-xs text-zinc-500 dark:text-zinc-400">
          Cabeçalho esperado: <code>date,description,amount</code> — data no formato aaaa-mm-dd, valor com ponto
          decimal (positivo = receita, negativo = despesa).
        </p>
      </div>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={uploadBatch.isPending} className="w-full">
          {uploadBatch.isPending ? 'Importando...' : 'Importar extrato'}
        </Button>
      </div>
    </form>
  )
}
