import { useMemo } from 'react'
import type { FaqCategory } from '../types'
import { normalizeSearchText } from '../utils/parseFaq'

interface FaqSearchResult {
  filteredCategories: FaqCategory[]
  totalMatches: number
  isSearching: boolean
}

export function useFaqSearch(categories: FaqCategory[], searchTerm: string): FaqSearchResult {
  return useMemo(() => {
    const term = normalizeSearchText(searchTerm.trim())
    if (!term) {
      const totalMatches = categories.reduce((sum, category) => sum + category.items.length, 0)
      return { filteredCategories: categories, totalMatches, isSearching: false }
    }

    const filteredCategories = categories
      .map((category) => ({
        ...category,
        items: category.items.filter(
          (item) =>
            normalizeSearchText(item.question).includes(term) || normalizeSearchText(item.answer).includes(term),
        ),
      }))
      .filter((category) => category.items.length > 0)

    const totalMatches = filteredCategories.reduce((sum, category) => sum + category.items.length, 0)
    return { filteredCategories, totalMatches, isSearching: true }
  }, [categories, searchTerm])
}
