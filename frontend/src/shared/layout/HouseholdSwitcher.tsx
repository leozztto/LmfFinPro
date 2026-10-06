import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { ChevronDownIcon, UsersIcon, UserIcon } from '@/shared/ui/icons'

const itemClassName =
  'flex w-full items-center gap-3 px-4 py-2 text-left text-sm hover:bg-zinc-100 focus:bg-zinc-100 focus:outline-none dark:hover:bg-zinc-700/60 dark:focus:bg-zinc-700/60'

/**
 * Alterna entre "Meus dados" e os grupos compartilhados (casal/família): é o que decide de quem são
 * as contas, transações e relatórios na tela. Só aparece para quem participa de algum grupo, para
 * não pesar o topo de quem usa o FinPro sozinho.
 */
export function HouseholdSwitcher() {
  const { sharedHouseholds, active, isShared, switchTo } = useHousehold()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return

    function handlePointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
        buttonRef.current?.focus()
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [open])

  if (sharedHouseholds.length === 0) return null

  const label = isShared ? (active?.name ?? 'Grupo') : 'Meus dados'
  const Icon = isShared ? UsersIcon : UserIcon

  function choose(householdId: number | null) {
    setOpen(false)
    if (householdId !== (isShared ? (active?.id ?? null) : null)) switchTo(householdId)
  }

  return (
    <div ref={containerRef} className="relative min-w-0">
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`Dados em exibição: ${label}. Trocar`}
        className={`flex max-w-[10.5rem] items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600 sm:max-w-[14rem] ${
          isShared
            ? 'border-[#2ad6a5] bg-[#2ad6a5]/10 text-[#1ea883] dark:text-[#2ad6a5]'
            : 'border-zinc-200 text-zinc-700 hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800'
        }`}
      >
        <Icon className="h-4 w-4 shrink-0" />
        <span className="truncate">{label}</span>
        <ChevronDownIcon className="h-3.5 w-3.5 shrink-0" />
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Escolher os dados em exibição"
          className="absolute right-0 z-40 mt-2 w-64 max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-zinc-200 bg-white py-1 shadow-lg dark:border-zinc-700 dark:bg-zinc-800"
        >
          <button
            type="button"
            role="menuitemradio"
            aria-checked={!isShared}
            onClick={() => choose(null)}
            className={`${itemClassName} ${!isShared ? 'font-medium text-[#1ea883] dark:text-[#2ad6a5]' : 'text-zinc-700 dark:text-zinc-200'}`}
          >
            <UserIcon className="h-4 w-4 shrink-0" />
            <span className="min-w-0 flex-1 truncate">Meus dados</span>
          </button>
          {sharedHouseholds.map((household) => {
            const selected = isShared && active?.id === household.id
            return (
              <button
                key={household.id}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                onClick={() => choose(household.id)}
                className={`${itemClassName} ${selected ? 'font-medium text-[#1ea883] dark:text-[#2ad6a5]' : 'text-zinc-700 dark:text-zinc-200'}`}
              >
                <UsersIcon className="h-4 w-4 shrink-0" />
                <span className="min-w-0 flex-1 truncate" title={household.name}>
                  {household.name}
                </span>
              </button>
            )
          })}
          <Link
            to="/configuracoes/grupos"
            role="menuitem"
            onClick={() => setOpen(false)}
            className={`${itemClassName} border-t border-zinc-200 text-zinc-700 dark:border-zinc-700 dark:text-zinc-200`}
          >
            Gerenciar grupos
          </Link>
        </div>
      )}
    </div>
  )
}
