import { useState } from 'react'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { TagInput } from '@/features/tags/components/TagInput'
import { useUpdateTransactionTags } from '@/features/transactions/hooks/useUpdateTransactionTags'
import type { Transaction } from '@/features/transactions/types'

/**
 * Tags de uma transação importada, na revisão do extrato. Salva a cada tag adicionada ou removida,
 * como os seletores de categoria e cliente ao lado; se o backend recusar, volta ao que estava.
 */
export function ImportReviewTags({ transaction }: { transaction: Transaction }) {
  const updateTags = useUpdateTransactionTags()
  const { showToast } = useToast()
  const [tagNames, setTagNames] = useState(() => (transaction.tags ?? []).map((tag) => tag.name))

  function handleChange(next: string[]) {
    const previous = tagNames
    setTagNames(next)
    updateTags.mutate(
      { id: transaction.id, tagNames: next },
      {
        onError: (error) => {
          setTagNames(previous)
          showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar as tags.')
        },
      },
    )
  }

  return (
    <div>
      <label htmlFor={`import-tags-${transaction.id}`} className="sr-only">
        Tags de {transaction.description}
      </label>
      <TagInput
        id={`import-tags-${transaction.id}`}
        value={tagNames}
        onChange={handleChange}
        placeholder="Tags (opcional)"
      />
    </div>
  )
}
