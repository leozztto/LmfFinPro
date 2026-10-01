import { Button } from './Button'
import { ChevronLeftIcon, ChevronRightIcon } from './icons'

interface PaginationProps {
  /** Página atual, a partir de 0. */
  page: number
  totalPages: number
  totalElements: number
  /** Nome dos itens no plural, para o resumo ("transações"). */
  itemsLabel: string
  onPageChange: (page: number) => void
  disabled?: boolean
}

export function Pagination({ page, totalPages, totalElements, itemsLabel, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) {
    return (
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        {totalElements} {itemsLabel}
      </p>
    )
  }

  return (
    <nav aria-label="Paginação" className="flex flex-col items-center gap-2 sm:flex-row sm:justify-between">
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Página {page + 1} de {totalPages} · {totalElements} {itemsLabel}
      </p>
      <div className="flex w-full gap-2 sm:w-auto">
        <Button
          variant="secondary"
          className="flex-1 gap-1 sm:flex-none"
          onClick={() => onPageChange(page - 1)}
          disabled={disabled || page <= 0}
        >
          <ChevronLeftIcon className="h-4 w-4" />
          Anterior
        </Button>
        <Button
          variant="secondary"
          className="flex-1 gap-1 sm:flex-none"
          onClick={() => onPageChange(page + 1)}
          disabled={disabled || page >= totalPages - 1}
        >
          Próxima
          <ChevronRightIcon className="h-4 w-4" />
        </Button>
      </div>
    </nav>
  )
}
