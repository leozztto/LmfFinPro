import { useState, type FormEvent } from 'react'
import { Button, ColorInput, FormField, IconButton, Input, Modal } from '@/shared/ui'
import { PencilIcon, TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useCreateTag, useDeleteTag, useTags, useUpdateTag } from '../hooks/useTags'
import type { Tag } from '../types'
import { normalizeTagName, tagLabel, validateTagName } from '../utils'
import { TagBadge } from './TagBadge'

const HEX_COLOR = /^#[0-9a-fA-F]{6}$/

/**
 * Aba "Tags" em Categorias. As tags nascem sozinhas ao serem digitadas numa transação; aqui dá para
 * criar direto, renomear, trocar a cor e excluir.
 */
export function TagsManager() {
  const { data: tags, isLoading } = useTags()
  const createTag = useCreateTag()
  const deleteTag = useDeleteTag()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [newName, setNewName] = useState('')
  const [editing, setEditing] = useState<Tag | null>(null)

  const newNameError = newName.trim() === '' ? null : validateTagName(normalizeTagName(newName))

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    const name = normalizeTagName(newName)
    if (validateTagName(name)) return
    try {
      await createTag.mutateAsync({ name })
      showToast(`Tag ${tagLabel(name)} criada.`, 'success')
      setNewName('')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar a tag.')
    }
  }

  async function handleDelete(tag: Tag) {
    const usage =
      tag.transactionCount === 0
        ? 'Ela não está em nenhuma transação.'
        : `Ela sai de ${tag.transactionCount} ${tag.transactionCount === 1 ? 'transação' : 'transações'}; os lançamentos continuam.`
    const confirmed = await confirm({
      title: 'Excluir tag',
      message: `Excluir ${tagLabel(tag.name)}? ${usage}`,
      confirmLabel: 'Excluir',
    })
    if (!confirmed) return
    deleteTag.mutate(tag.id, {
      onSuccess: () => showToast('Tag excluída.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível excluir a tag.'),
    })
  }

  return (
    <div className="space-y-6">
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Tags classificam transações em qualquer categoria — por exemplo, um projeto (#site-acme) ou o que é dedutível no
        IR (#dedutível). Uma transação pode ter várias.
      </p>

      <form onSubmit={handleCreate} className="flex flex-col gap-2 sm:flex-row sm:items-start">
        <div className="min-w-0 flex-1">
          <label htmlFor="new-tag-name" className="sr-only">
            Nome da nova tag
          </label>
          <Input
            id="new-tag-name"
            placeholder="Nova tag, ex.: site-acme"
            value={newName}
            onChange={(event) => setNewName(event.target.value)}
          />
          {newNameError && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{newNameError}</p>}
        </div>
        <Button
          type="submit"
          className="w-full sm:w-auto"
          disabled={newName.trim() === '' || newNameError !== null || createTag.isPending}
        >
          Criar tag
        </Button>
      </form>

      {isLoading ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando tags...</p>
      ) : !tags?.length ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Nenhuma tag ainda. Crie acima ou digite uma tag nova ao lançar uma transação.
        </p>
      ) : (
        <ul className="grid grid-cols-1 gap-2 md:grid-cols-2">
          {tags.map((tag) => (
            <li
              key={tag.id}
              className="flex min-w-0 items-center gap-3 rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2.5 dark:border-zinc-700 dark:bg-zinc-800"
            >
              <div className="min-w-0 flex-1">
                <TagBadge name={tag.name} color={tag.color} />
                <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                  {tag.transactionCount === 0
                    ? 'Sem transações'
                    : `${tag.transactionCount} ${tag.transactionCount === 1 ? 'transação' : 'transações'}`}
                </p>
              </div>
              <div className="flex shrink-0 gap-1">
                <IconButton icon={PencilIcon} label={`Editar ${tagLabel(tag.name)}`} onClick={() => setEditing(tag)} />
                <IconButton
                  icon={TrashIcon}
                  label={`Excluir ${tagLabel(tag.name)}`}
                  onClick={() => handleDelete(tag)}
                  disabled={deleteTag.isPending}
                />
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal open={editing !== null} onClose={() => setEditing(null)} title="Editar tag">
        {editing && <TagEditForm key={editing.id} tag={editing} onDone={() => setEditing(null)} />}
      </Modal>
    </div>
  )
}

function TagEditForm({ tag, onDone }: { tag: Tag; onDone: () => void }) {
  const updateTag = useUpdateTag()
  const { showToast } = useToast()
  const [name, setName] = useState(tag.name)
  const [color, setColor] = useState(tag.color ?? '')

  const normalized = normalizeTagName(name)
  const nameError = validateTagName(normalized)
  const colorError = color.trim() !== '' && !HEX_COLOR.test(color.trim()) ? 'Use o formato #RRGGBB.' : null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (nameError || colorError) return
    try {
      await updateTag.mutateAsync({ id: tag.id, input: { name: normalized, color: color.trim() || null } })
      showToast('Tag atualizada.', 'success')
      onDone()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar a tag.')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <FormField
        label="Nome"
        htmlFor="edit-tag-name"
        error={nameError ?? undefined}
        hint={normalized && normalized !== name ? `Fica ${tagLabel(normalized)}` : undefined}
      >
        <Input id="edit-tag-name" value={name} onChange={(event) => setName(event.target.value)} />
      </FormField>
      <FormField label="Cor (opcional)" htmlFor="edit-tag-color" error={colorError ?? undefined}>
        <ColorInput id="edit-tag-color" value={color} onChange={setColor} placeholder="#2ad6a5" />
      </FormField>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Renomear muda a tag em todas as transações que a usam.
      </p>
      <Button type="submit" className="w-full" disabled={updateTag.isPending || nameError !== null || colorError !== null}>
        {updateTag.isPending ? 'Salvando...' : 'Salvar'}
      </Button>
    </form>
  )
}
