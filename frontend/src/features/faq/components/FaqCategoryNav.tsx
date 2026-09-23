import type { FaqCategory } from '../types'

interface FaqCategoryNavProps {
  categories: FaqCategory[]
}

function scrollToCategory(slug: string) {
  document.getElementById(slug)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

export function FaqCategoryNav({ categories }: FaqCategoryNavProps) {
  return (
    <>
      <nav className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:hidden" aria-label="Categorias do FAQ">
        {categories.map((category) => (
          <button
            key={category.slug}
            type="button"
            onClick={() => scrollToCategory(category.slug)}
            className="shrink-0 whitespace-nowrap rounded-full border border-zinc-200 px-3 py-1.5 text-xs font-medium text-zinc-600 hover:border-zinc-300 hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800"
          >
            {category.name}
          </button>
        ))}
      </nav>

      <nav className="hidden lg:sticky lg:top-6 lg:block lg:self-start" aria-label="Categorias do FAQ">
        <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
          Categorias
        </p>
        <div className="flex flex-col gap-1">
          {categories.map((category) => (
            <button
              key={category.slug}
              type="button"
              onClick={() => scrollToCategory(category.slug)}
              className="flex items-center justify-between gap-2 rounded-md px-3 py-2 text-left text-sm text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/60"
            >
              <span className="truncate">{category.name}</span>
              <span className="shrink-0 text-xs text-zinc-400 dark:text-zinc-500">{category.items.length}</span>
            </button>
          ))}
        </div>
      </nav>
    </>
  )
}
