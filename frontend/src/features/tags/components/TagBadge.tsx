import { tagLabel } from '../utils'

interface TagBadgeProps {
  name: string
  color?: string | null
  /** Botão de remover dentro da etiqueta (campo de tags). */
  onRemove?: () => void
}

/** Etiqueta "#nome"; a cor da tag, quando definida, aparece como um ponto antes do nome. */
export function TagBadge({ name, color, onRemove }: TagBadgeProps) {
  return (
    <span className="inline-flex max-w-full items-center gap-1 rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-700 dark:bg-zinc-700 dark:text-zinc-200">
      {color && <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: color }} aria-hidden="true" />}
      <span className="truncate">{tagLabel(name)}</span>
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          aria-label={`Remover ${tagLabel(name)}`}
          className="-mr-0.5 shrink-0 rounded-full px-0.5 leading-none text-zinc-500 hover:bg-zinc-200 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-600 dark:hover:text-zinc-100"
        >
          ×
        </button>
      )}
    </span>
  )
}
