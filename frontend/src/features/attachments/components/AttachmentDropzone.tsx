import { useState, type DragEvent } from 'react'
import { UploadIcon } from '@/shared/ui/icons'
import { ACCEPT_ATTRIBUTE } from '../utils'

interface AttachmentDropzoneProps {
  id: string
  onFilesSelected: (files: File[]) => void
  disabled?: boolean
}

/**
 * Área para escolher comprovantes: clique abre o seletor do sistema (várias seleções de uma vez) e,
 * no desktop, também dá para arrastar os arquivos até ela.
 */
export function AttachmentDropzone({ id, onFilesSelected, disabled = false }: AttachmentDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false)

  function handleDragOver(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    if (!disabled) setIsDragging(true)
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setIsDragging(false)
    if (!disabled) onFilesSelected(Array.from(event.dataTransfer.files))
  }

  const stateClass = disabled
    ? 'cursor-not-allowed opacity-60'
    : isDragging
      ? 'border-primary-500 bg-primary-50 dark:border-[#2ad6a5] dark:bg-[#2ad6a5]/10'
      : 'cursor-pointer hover:border-primary-400 hover:bg-white dark:hover:border-zinc-500 dark:hover:bg-zinc-900'

  return (
    <label
      htmlFor={id}
      onDragOver={handleDragOver}
      onDragLeave={() => setIsDragging(false)}
      onDrop={handleDrop}
      className={`flex w-full flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-zinc-300 px-4 py-5 text-center transition-colors focus-within:outline focus-within:outline-2 focus-within:outline-primary-500 dark:border-zinc-600 ${stateClass}`}
    >
      <UploadIcon className="h-6 w-6 text-zinc-400 dark:text-zinc-500" aria-hidden="true" />
      <span className="text-sm font-medium text-zinc-700 dark:text-zinc-200">
        Escolher arquivos<span className="hidden sm:inline"> ou arrastar até aqui</span>
      </span>
      <span className="text-xs text-zinc-500 dark:text-zinc-400">PDF, JPG, PNG ou WEBP · até 10 MB cada</span>
      <input
        id={id}
        type="file"
        multiple
        accept={ACCEPT_ATTRIBUTE}
        disabled={disabled}
        className="sr-only"
        onChange={(event) => {
          onFilesSelected(Array.from(event.target.files ?? []))
          // Limpa para que escolher o mesmo arquivo de novo também dispare o onChange.
          event.target.value = ''
        }}
      />
    </label>
  )
}
