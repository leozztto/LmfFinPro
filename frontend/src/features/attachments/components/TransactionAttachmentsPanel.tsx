import { useRef, useState, type FormEvent } from 'react'
import { Button, FileInput, FormField, IconButton, Select } from '@/shared/ui'
import { DownloadIcon, EyeIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { formatDateOnlyBr } from '@/shared/format/date'
import { attachmentsApi } from '../api/attachmentsApi'
import { useDeleteAttachment, useTransactionAttachments, useUploadAttachment } from '../hooks/useTransactionAttachments'
import { ATTACHMENT_DOCUMENT_TYPE_LABELS, type AttachmentDocumentType, type TransactionAttachment } from '../types'
import { ACCEPT_ATTRIBUTE, MAX_ATTACHMENTS_PER_TRANSACTION, formatFileSize, validateAttachmentFile } from '../utils'

interface TransactionAttachmentsPanelProps {
  transactionId: number
}

/** Conteúdo do modal "Comprovantes": enviar, ver, baixar e excluir os anexos de uma transação. */
export function TransactionAttachmentsPanel({ transactionId }: TransactionAttachmentsPanelProps) {
  const { data: attachments, isLoading } = useTransactionAttachments(transactionId)
  const uploadAttachment = useUploadAttachment(transactionId)
  const deleteAttachment = useDeleteAttachment(transactionId)
  const { showToast } = useToast()
  const confirm = useConfirm()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [documentType, setDocumentType] = useState<AttachmentDocumentType>('PAYMENT_PROOF')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)

  const count = attachments?.length ?? 0

  function handleFileChange(selected: File | null) {
    setFile(selected)
    setError(selected ? validateAttachmentFile(selected, count) : null)
  }

  async function handleUpload(event: FormEvent) {
    event.preventDefault()
    if (!file) return
    const validation = validateAttachmentFile(file, count)
    if (validation) {
      setError(validation)
      return
    }
    try {
      await uploadAttachment.mutateAsync({ file, documentType })
      showToast('Comprovante anexado.', 'success')
      setFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
    } catch (uploadError) {
      setError(uploadError instanceof ApiError ? uploadError.message : 'Não foi possível enviar o arquivo.')
    }
  }

  /**
   * A janela é aberta antes de buscar o arquivo (ainda dentro do clique), senão o navegador a
   * bloqueia como pop-up. Se mesmo assim for bloqueada, o arquivo é baixado.
   */
  async function handleView(attachment: TransactionAttachment) {
    const preview = window.open('', '_blank')
    try {
      const blob = await attachmentsApi.content(transactionId, attachment.id)
      const url = URL.createObjectURL(blob)
      if (preview) {
        preview.location.href = url
      } else {
        downloadBlob(blob, attachment.fileName)
      }
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (viewError) {
      preview?.close()
      showToast(viewError instanceof ApiError ? viewError.message : 'Não foi possível abrir o arquivo.')
    }
  }

  async function handleDownload(attachment: TransactionAttachment) {
    try {
      downloadBlob(await attachmentsApi.content(transactionId, attachment.id, true), attachment.fileName)
    } catch (downloadError) {
      showToast(downloadError instanceof ApiError ? downloadError.message : 'Não foi possível baixar o arquivo.')
    }
  }

  async function handleDelete(attachment: TransactionAttachment) {
    const confirmed = await confirm({
      message: `Excluir o anexo "${attachment.fileName}"? Essa ação não pode ser desfeita.`,
    })
    if (!confirmed) return
    deleteAttachment.mutate(attachment.id, {
      onSuccess: () => showToast('Anexo excluído.', 'success'),
      onError: (deleteError) =>
        showToast(deleteError instanceof ApiError ? deleteError.message : 'Não foi possível excluir o anexo.'),
    })
  }

  return (
    <div className="space-y-5">
      <form onSubmit={handleUpload} className="space-y-3">
        <div className="grid gap-3 sm:grid-cols-2">
          <FormField label="Tipo de documento" htmlFor="attachment-type">
            <Select
              id="attachment-type"
              value={documentType}
              onChange={(event) => setDocumentType(event.target.value as AttachmentDocumentType)}
            >
              {Object.entries(ATTACHMENT_DOCUMENT_TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Arquivo (PDF, JPG, PNG ou WEBP, até 10 MB)" htmlFor="attachment-file">
            <FileInput
              id="attachment-file"
              ref={fileInputRef}
              accept={ACCEPT_ATTRIBUTE}
              onChange={(event) => handleFileChange(event.target.files?.[0] ?? null)}
            />
          </FormField>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button
          type="submit"
          className="w-full sm:w-auto"
          disabled={!file || error !== null || uploadAttachment.isPending || count >= MAX_ATTACHMENTS_PER_TRANSACTION}
        >
          {uploadAttachment.isPending ? 'Enviando...' : 'Anexar'}
        </Button>
      </form>

      <div>
        <p className="mb-2 text-sm font-medium text-zinc-700 dark:text-zinc-200">
          Anexos ({count}/{MAX_ATTACHMENTS_PER_TRANSACTION})
        </p>
        {isLoading ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>
        ) : count === 0 ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Nenhum comprovante anexado. Anexe a nota fiscal, o recibo ou o comprovante de pagamento — eles entram no
            pacote de comprovantes do ano, em Relatórios.
          </p>
        ) : (
          <ul className="divide-y divide-zinc-200 dark:divide-zinc-700">
            {attachments?.map((attachment) => (
              <li key={attachment.id} className="flex items-center justify-between gap-3 py-2">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-zinc-800 dark:text-zinc-100" title={attachment.fileName}>
                    {attachment.fileName}
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {ATTACHMENT_DOCUMENT_TYPE_LABELS[attachment.documentType]} · {formatFileSize(attachment.sizeBytes)} ·{' '}
                    {formatDateOnlyBr(attachment.createdAt.slice(0, 10))}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1">
                  <IconButton icon={EyeIcon} label="Ver" onClick={() => handleView(attachment)} />
                  <IconButton icon={DownloadIcon} label="Baixar" onClick={() => handleDownload(attachment)} />
                  <IconButton
                    icon={TrashIcon}
                    label="Excluir"
                    onClick={() => handleDelete(attachment)}
                    disabled={deleteAttachment.isPending}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
