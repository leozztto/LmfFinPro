import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'

/** Pacote para o IR / contador: todos os comprovantes do ano num ZIP, com um índice em CSV. */
export function AttachmentsArchiveForm() {
  const { showToast } = useToast()
  const [year, setYear] = useState(String(new Date().getFullYear()))
  const download = useMutation({
    mutationFn: async (selectedYear: string) =>
      downloadBlob(await reportsApi.downloadAttachmentsArchive(selectedYear), `comprovantes-${selectedYear}.zip`),
  })
  const isValidYear = /^\d{4}$/.test(year)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!isValidYear) return
    try {
      await download.mutateAsync(year)
      showToast('Pacote de comprovantes gerado.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível gerar o pacote de comprovantes.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Ano" htmlFor="attachments-archive-year">
        <Input
          id="attachments-archive-year"
          type="number"
          inputMode="numeric"
          min={2000}
          max={2100}
          value={year}
          onChange={(event) => setYear(event.target.value)}
        />
      </FormField>
      <div className="sm:col-span-2">
        <p className="mb-3 text-xs text-zinc-500 dark:text-zinc-400">
          Um arquivo ZIP com todos os comprovantes, notas fiscais e recibos anexados às transações do ano, em pastas por
          mês, e uma planilha (CSV) que liga cada arquivo à transação — data, descrição, valor, categoria e cliente. É o
          que você entrega ao contador para a declaração de IR.
        </p>
        <Button type="submit" disabled={download.isPending || !isValidYear} className="w-full">
          {download.isPending ? 'Gerando...' : 'Baixar comprovantes do ano (ZIP)'}
        </Button>
      </div>
    </form>
  )
}
