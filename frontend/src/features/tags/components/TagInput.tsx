import { useId, useMemo, useState, type KeyboardEvent } from 'react'
import { useTags } from '../hooks/useTags'
import { MAX_TAGS_PER_ITEM, normalizeTagName, tagLabel, validateTagName } from '../utils'
import { TagBadge } from './TagBadge'

interface TagInputProps {
  id: string
  /** Nomes das tags escolhidas (normalizados, sem "#"). */
  value: string[]
  onChange: (names: string[]) => void
  /** Falso nos filtros: só dá para escolher tags que já existem. */
  allowCreate?: boolean
  placeholder?: string
  disabled?: boolean
}

type Option = { name: string; color: string | null; isNew: boolean }

/**
 * Campo de tags: as escolhidas viram etiquetas e, ao digitar, aparecem as tags existentes que
 * combinam. Enter, vírgula ou Tab adicionam o que foi digitado (criando a tag, se `allowCreate`);
 * Backspace com o campo vazio tira a última.
 */
export function TagInput({ id, value, onChange, allowCreate = true, placeholder, disabled = false }: TagInputProps) {
  const { data: tags } = useTags()
  const listboxId = useId()
  const [query, setQuery] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const [highlighted, setHighlighted] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const normalizedQuery = normalizeTagName(query)
  const isFull = value.length >= MAX_TAGS_PER_ITEM
  const colorByName = useMemo(() => new Map((tags ?? []).map((tag) => [tag.name, tag.color])), [tags])

  const options = useMemo<Option[]>(() => {
    const available = (tags ?? [])
      .filter((tag) => !value.includes(tag.name))
      .filter((tag) => normalizedQuery === '' || tag.name.includes(normalizedQuery))
      .slice(0, 8)
      .map((tag) => ({ name: tag.name, color: tag.color, isNew: false }))
    const exists = (tags ?? []).some((tag) => tag.name === normalizedQuery)
    // "Criar" vem primeiro: Enter adiciona o que foi digitado; as setas levam às sugestões.
    if (allowCreate && normalizedQuery !== '' && !exists && !value.includes(normalizedQuery)) {
      return [{ name: normalizedQuery, color: null, isNew: true }, ...available]
    }
    return available
  }, [tags, value, normalizedQuery, allowCreate])

  function add(rawName: string) {
    const name = normalizeTagName(rawName)
    if (name === '') return
    if (value.includes(name)) {
      setQuery('')
      return
    }
    const validation = validateTagName(name)
    if (validation) {
      setError(validation)
      return
    }
    if (!allowCreate && !colorByName.has(name)) {
      setError('Escolha uma das tags da lista.')
      return
    }
    if (isFull) {
      setError(`Use no máximo ${MAX_TAGS_PER_ITEM} tags.`)
      return
    }
    onChange([...value, name])
    setQuery('')
    setError(null)
    setHighlighted(0)
  }

  function remove(name: string) {
    onChange(value.filter((current) => current !== name))
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      setIsOpen(true)
      const step = event.key === 'ArrowDown' ? 1 : -1
      setHighlighted((current) => (options.length === 0 ? 0 : (current + step + options.length) % options.length))
      return
    }
    if (event.key === 'Enter' || event.key === ',' || (event.key === 'Tab' && query.trim() !== '')) {
      if (event.key === 'Enter' && query.trim() === '') return
      event.preventDefault()
      const option = isOpen ? options[highlighted] : undefined
      add(option ? option.name : query)
      return
    }
    if (event.key === 'Backspace' && query === '' && value.length > 0) {
      remove(value[value.length - 1])
      return
    }
    if (event.key === 'Escape') {
      setIsOpen(false)
    }
  }

  const showList = isOpen && !disabled && options.length > 0

  return (
    <div className="relative">
      <div
        className={`flex min-h-[38px] w-full flex-wrap items-center gap-1 rounded-lg border bg-white px-2 py-1.5 text-sm focus-within:border-primary-500 focus-within:outline focus-within:outline-2 focus-within:outline-primary-100 dark:bg-zinc-900 dark:focus-within:border-zinc-500 dark:focus-within:outline-zinc-700 ${
          error ? 'border-red-400 dark:border-red-500' : 'border-zinc-200 dark:border-zinc-700'
        } ${disabled ? 'opacity-60' : ''}`}
      >
        {value.map((name) => (
          <TagBadge
            key={name}
            name={name}
            color={colorByName.get(name)}
            onRemove={disabled ? undefined : () => remove(name)}
          />
        ))}
        <input
          id={id}
          role="combobox"
          aria-expanded={showList}
          aria-controls={listboxId}
          aria-autocomplete="list"
          autoComplete="off"
          disabled={disabled || isFull}
          value={query}
          placeholder={value.length === 0 ? placeholder : isFull ? undefined : 'Adicionar…'}
          onChange={(event) => {
            setQuery(event.target.value)
            setIsOpen(true)
            setHighlighted(0)
            setError(null)
          }}
          onFocus={() => setIsOpen(true)}
          onBlur={() => {
            setIsOpen(false)
            // Sair do campo com algo digitado adiciona a tag, para ela não se perder no envio.
            if (query.trim() !== '') add(query)
          }}
          onKeyDown={handleKeyDown}
          className="min-w-[7rem] flex-1 bg-transparent px-1 py-0.5 text-zinc-800 outline-none placeholder:text-zinc-400 disabled:cursor-not-allowed dark:text-zinc-100 dark:placeholder:text-zinc-500"
        />
      </div>

      {showList && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-lg border border-zinc-200 bg-white py-1 shadow-lg dark:border-zinc-700 dark:bg-zinc-900"
        >
          {options.map((option, index) => (
            <li
              key={`${option.isNew ? 'new' : 'tag'}-${option.name}`}
              role="option"
              aria-selected={index === highlighted}
              // mousedown em vez de click: acontece antes do blur, então o campo não fecha a lista antes.
              onMouseDown={(event) => {
                event.preventDefault()
                add(option.name)
              }}
              onMouseEnter={() => setHighlighted(index)}
              className={`flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm ${
                index === highlighted
                  ? 'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-50'
                  : 'text-zinc-700 dark:text-zinc-200'
              }`}
            >
              {option.isNew ? (
                <span className="truncate">
                  Criar <strong>{tagLabel(option.name)}</strong>
                </span>
              ) : (
                <>
                  <span
                    className="h-2 w-2 shrink-0 rounded-full bg-zinc-300 dark:bg-zinc-600"
                    style={option.color ? { backgroundColor: option.color } : undefined}
                    aria-hidden="true"
                  />
                  <span className="truncate">{tagLabel(option.name)}</span>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
      {error && <p className="mt-1 text-xs text-red-600 dark:text-red-400">{error}</p>}
    </div>
  )
}
