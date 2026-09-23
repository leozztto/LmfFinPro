import { useState } from 'react'
import { FormField, Input } from '@/shared/ui'
import { FAQ_DATA } from '../data'
import { useFaqSearch } from '../hooks/useFaqSearch'
import { FaqCategoryNav } from './FaqCategoryNav'
import { FaqCategorySection } from './FaqCategorySection'

export function FaqPage() {
  const [searchTerm, setSearchTerm] = useState('')
  const { filteredCategories, totalMatches, isSearching } = useFaqSearch(FAQ_DATA.categories, searchTerm)

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Perguntas Frequentes</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">{FAQ_DATA.subtitle}</p>
      </div>

      <div className="max-w-md">
        <FormField label="Buscar" htmlFor="faq-search">
          <Input
            id="faq-search"
            type="search"
            placeholder="Buscar pergunta ou resposta..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </FormField>
        {isSearching && (
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            {totalMatches} {totalMatches === 1 ? 'pergunta encontrada' : 'perguntas encontradas'}
          </p>
        )}
      </div>

      {filteredCategories.length === 0 ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma pergunta encontrada para "{searchTerm.trim()}".
        </p>
      ) : (
        <div className="lg:grid lg:grid-cols-[200px_1fr] lg:items-start lg:gap-6">
          <FaqCategoryNav categories={filteredCategories} />
          <div className="mt-4 space-y-4 lg:mt-0">
            {filteredCategories.map((category) => (
              <FaqCategorySection key={category.slug} category={category} forceOpen={isSearching} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
