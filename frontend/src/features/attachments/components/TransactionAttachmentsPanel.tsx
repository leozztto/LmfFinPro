import { Button, IconButton } from '@/shared/ui'
import { DownloadIcon, EyeIcon, FileTextIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { formatDateOnlyBr } from '@/shared/format/date'
import { attachmentsApi } from '../api/attachmentsApi'
import { usePendingAttachments } from '../hooks/usePendingAttachments'
import {
  useDeleteAttachment,
  useTransactionAttachments,
  useUploadPendingAttachments,
} from '../hooks/useTransactionAttachments'
import { ATTACHMENT_DOCUMENT_TYPE_LABELS, type TransactionAttachment } from '../types'
import { MAX_ATTACHMENTS_PER_TRANSACTION, formatFileSize } from '../utils'
import { AttachmentDropzone } from './AttachmentDropzone'
import { PendingAttachmentList } from './PendingAttachmentList'

interface TransactionAttachmentsPanelProps {
  transactionId: number
}

/** Conteúdo do modal "Comprovantes": ver, baixar e excluir os anexos e enviar novos. */
export function TransactionAttachmentsPanel({ transactionId }: TransactionAttachmentsPanelProps) {
  const { data: attachments, isLoading } = useTransactionAttachments(transactionId)
  const uploadPending = useUploadPendingAttachments()
  const deleteAttachment = useDeleteAttachment(transactionId)
  const { showToast } = useToast()
  const confirm = useConfirm()

  const count = attachments?.length ?? 0
  const pending = usePendingAttachments(count)
  const isFull = count + pending.items.length >= MAX_ATTACHMENTS_PER_TRANSACTION

  async function handleUpload() {
    const { uploadedCount, failed } = await uploadPending.mutateAsync({ transactionId, items: pending.items })
    pending.replace(failed)
    if (uploadedCount > 0) {
      showToast(uploadedCount === 1 ? 'Comprovante anexado.' : `${uploadedCount} comprovantes anexados.`, 'success')
    }
    if (failed.length > 0) {
      showToast(
        failed.length === 1
          ? 'Um arquivo não foi enviado. Veja o motivo na lista.'
          : `${failed.length} arquivos não foram enviados. Veja o motivo na lista.`,
      )
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
    <div className="space-y-6">
      <section className="space-y-2">
        <h4 className="text-sm font-medium text-zinc-700 dark:text-zinc-200">
          Anexados ({count}/{MAX_ATTACHMENTS_PER_TRANSACTION})
        </h4>
        {isLoading ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>
        ) : count === 0 ? (
          <p className="rounded-lg bg-white px-3 py-2.5 text-sm text-zinc-500 dark:bg-zinc-900 dark:text-zinc-400">
            Nenhum comprovante ainda. Eles entram no pacote de comprovantes do ano, em Relatórios.
          </p>
        ) : (
          <ul className="space-y-2">
            {attachments?.map((attachment) => (
              <li
                key={attachment.id}
                className="rounded-lg border border-zinc-200 bg-white p-2.5 dark:border-zinc-700 dark:bg-zinc-900"
              >
                <div className="flex items-center gap-2">
                  <FileTextIcon className="h-4 w-4 shrink-0 text-zinc-400" aria-hidden="true" />
                  <div className="min-w-0 flex-1">
                    <p
                      className="truncate text-sm font-medium text-zinc-800 dark:text-zinc-100"
                      title={attachment.fileName}
                    >
                      {attachment.fileName}
                    </p>
                    <p className="truncate text-xs text-zinc-500 dark:text-zinc-400">
                      {ATTACHMENT_DOCUMENT_TYPE_LABELS[attachment.documentType]} ·{' '}
                      {formatFileSize(attachment.sizeBytes)} · {formatDateOnlyBr(attachment.createdAt.slice(0, 10))}
                    </p>
                  </div>
                  {/* Desktop: ícones ao lado do nome. */}
                  <div className="hidden shrink-0 items-center gap-1 sm:flex">
                    <IconButton
                      icon={EyeIcon}
                      label={`Ver ${attachment.fileName}`}
                      onClick={() => handleView(attachment)}
                    />
                    <IconButton
                      icon={DownloadIcon}
                      label={`Baixar ${attachment.fileName}`}
                      onClick={() => handleDownload(attachment)}
                    />
                    <IconButton
                      icon={TrashIcon}
                      label={`Excluir ${attachment.fileName}`}
                      onClick={() => handleDelete(attachment)}
                      disabled={deleteAttachment.isPending}
                    />
                  </div>
                </div>
                {/* Celular: ações numa linha própria, com texto, para o nome ter a largura toda. */}
                <div className="mt-2 grid grid-cols-3 gap-2 sm:hidden">
                  <Button variant="secondary" className="gap-1.5 px-2 py-1.5" onClick={() => handleView(attachment)}>
                    <EyeIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" /> Ver
                  </Button>
                  <Button variant="secondary" className="gap-1.5 px-2 py-1.5" onClick={() => handleDownload(attachment)}>
                    <DownloadIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" /> Baixar
                  </Button>
                  <Button
                    variant="secondary"
                    className="gap-1.5 px-2 py-1.5"
                    onClick={() => handleDelete(attachment)}
                    disabled={deleteAttachment.isPending}
                  >
                    <TrashIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" /> Excluir
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="space-y-3 border-t border-zinc-200 pt-5 dark:border-zinc-700">
        <h4 className="text-sm font-medium text-zinc-700 dark:text-zinc-200">Adicionar comprovantes</h4>
        {count >= MAX_ATTACHMENTS_PER_TRANSACTION ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Esta transação já tem o máximo de {MAX_ATTACHMENTS_PER_TRANSACTION} anexos. Exclua um para enviar outro.
          </p>
        ) : (
          <>
            <AttachmentDropzone
              id={`attachment-files-${transactionId}`}
              onFilesSelected={pending.add}
              disabled={isFull || uploadPending.isPending}
            />
            <PendingAttachmentList
              items={pending.items}
              rejections={pending.rejections}
              onChangeType={pending.changeType}
              onRemove={pending.remove}
              disabled={uploadPending.isPending}
            />
            {pending.items.length > 0 && (
              <div className="flex justify-end">
                <Button
                  type="button"
                  onClick={handleUpload}
                  disabled={uploadPending.isPending}
                  className="w-full sm:w-auto"
                >
                  {uploadPending.isPending
                    ? 'Enviando...'
                    : pending.items.length === 1
                      ? 'Enviar 1 arquivo'
                      : `Enviar ${pending.items.length} arquivos`}
                </Button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}
