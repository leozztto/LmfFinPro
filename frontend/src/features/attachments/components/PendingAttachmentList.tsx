import { IconButton, Select } from '@/shared/ui'
import { CloseIcon, FileTextIcon } from '@/shared/ui/icons'
import { ATTACHMENT_DOCUMENT_TYPE_LABELS, type AttachmentDocumentType, type PendingAttachment } from '../types'
import { formatFileSize } from '../utils'

interface PendingAttachmentListProps {
  items: PendingAttachment[]
  rejections: string[]
  onChangeType: (key: number, documentType: AttachmentDocumentType) => void
  onRemove: (key: number) => void
  disabled?: boolean
}

/**
 * Fila de arquivos escolhidos, cada um com o seu tipo de documento. No celular o tipo desce para a
 * linha de baixo, ocupando a largura toda; a partir de {@code sm} fica tudo numa linha só.
 */
export function PendingAttachmentList({
  items,
  rejections,
  onChangeType,
  onRemove,
  disabled = false,
}: PendingAttachmentListProps) {
  if (items.length === 0 && rejections.length === 0) return null

  return (
    <div className="space-y-2">
      {rejections.length > 0 && (
        <ul className="space-y-1 text-sm text-red-600 dark:text-red-400" role="alert">
          {rejections.map((rejection) => (
            <li key={rejection} className="break-words">
              {rejection}
            </li>
          ))}
        </ul>
      )}
      {items.length > 0 && (
        <ul className="space-y-2">
          {items.map((item) => (
            <li
              key={item.key}
              className={`rounded-lg border bg-white p-2.5 dark:bg-zinc-900 ${
                item.error ? 'border-red-300 dark:border-red-500/60' : 'border-zinc-200 dark:border-zinc-700'
              }`}
            >
              <div className="flex flex-wrap items-center gap-2">
                <FileTextIcon className="h-4 w-4 shrink-0 text-zinc-400" aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-zinc-800 dark:text-zinc-100" title={item.file.name}>
                    {item.file.name}
                  </p>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">{formatFileSize(item.file.size)}</p>
                </div>
                <IconButton
                  icon={CloseIcon}
                  label={`Tirar ${item.file.name} da lista`}
                  onClick={() => onRemove(item.key)}
                  disabled={disabled}
                  className="sm:order-last"
                />
                <label htmlFor={`pending-attachment-${item.key}`} className="sr-only">
                  Tipo de documento de {item.file.name}
                </label>
                <Select
                  id={`pending-attachment-${item.key}`}
                  value={item.documentType}
                  disabled={disabled}
                  onChange={(event) => onChangeType(item.key, event.target.value as AttachmentDocumentType)}
                  className="basis-full sm:w-60 sm:basis-auto"
                >
                  {Object.entries(ATTACHMENT_DOCUMENT_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Select>
              </div>
              {item.error && <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">{item.error}</p>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
