import { useState, type FormEvent } from 'react'
import { Button, FormField } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { TagInput } from '@/features/tags/components/TagInput'
import { useUpdateTransactionTags } from '../hooks/useUpdateTransactionTags'
import type { Transaction } from '../types'

interface TransactionTagsEditorProps {
  transaction: Transaction
  onDone: () => void
}

/** Conteúdo do modal "Tags" de uma transação já lançada. */
export function TransactionTagsEditor({ transaction, onDone }: TransactionTagsEditorProps) {
  const updateTags = useUpdateTransactionTags()
  const { showToast } = useToast()
  const [tagNames, setTagNames] = useState(() => (transaction.tags ?? []).map((tag) => tag.name))

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    try {
      await updateTags.mutateAsync({ id: transaction.id, tagNames })
      showToast('Tags atualizadas.', 'success')
      onDone()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar as tags.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <FormField
        label="Tags"
        htmlFor={`transaction-${transaction.id}-tags`}
        hint="Enter ou vírgula adiciona; tag nova é criada na hora. Deixe vazio para tirar todas."
      >
        <TagInput
          id={`transaction-${transaction.id}-tags`}
          value={tagNames}
          onChange={setTagNames}
          placeholder="Digite uma tag"
          disabled={updateTags.isPending}
        />
      </FormField>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" className="w-full sm:w-auto" onClick={onDone}>
          Cancelar
        </Button>
        <Button type="submit" className="w-full sm:w-auto" disabled={updateTags.isPending}>
          {updateTags.isPending ? 'Salvando...' : 'Salvar tags'}
        </Button>
      </div>
    </form>
  )
}
