import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/shared/auth/AuthContext'
import { UserAvatar } from '@/features/profile/components/UserAvatar'
import { useProfilePhotoUrl } from '@/features/profile/hooks/useProfilePhoto'
import { LogOutIcon, SettingsIcon, UsersIcon } from '@/shared/ui/icons'
import { AvatarInvitesBadge } from '@/features/households/components/AvatarInvitesBadge'
import { PendingInvitesBadge } from '@/features/households/components/PendingInvitesBadge'
import { pendingInvitesLabel } from '@/features/households/badgeCount'
import { useReceivedInvites } from '@/features/households/hooks/useHouseholds'

const itemClassName =
  'flex w-full items-center gap-3 px-4 py-2 text-left text-sm text-zinc-700 hover:bg-zinc-100 focus:bg-zinc-100 focus:outline-none dark:text-zinc-200 dark:hover:bg-zinc-700/60 dark:focus:bg-zinc-700/60'

/** Ícone de usuário no topo: abre um menu com Configurações e Sair. */
export function UserMenu() {
  const { session, logout } = useAuth()
  const photoUrl = useProfilePhotoUrl()
  const { data: receivedInvites } = useReceivedInvites()
  const pendingInvites = receivedInvites?.length ?? 0
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

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={
          pendingInvites > 0 ? `Menu do usuário, ${pendingInvitesLabel(pendingInvites)}` : 'Menu do usuário'
        }
        title={session?.name}
        className="relative rounded-full transition-opacity hover:opacity-80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600"
      >
        <UserAvatar photoUrl={photoUrl} name={session?.name} />
        <AvatarInvitesBadge count={pendingInvites} />
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Menu do usuário"
          className="absolute right-0 z-40 mt-2 w-60 max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-zinc-200 bg-white py-1 shadow-lg dark:border-zinc-700 dark:bg-zinc-800"
        >
          <div className="border-b border-zinc-200 px-4 py-3 dark:border-zinc-700">
            <p className="truncate text-sm font-medium text-zinc-800 dark:text-zinc-100">{session?.name}</p>
            <p className="truncate text-xs text-zinc-500 dark:text-zinc-400">{session?.email}</p>
          </div>
          <Link to="/configuracoes" role="menuitem" onClick={() => setOpen(false)} className={itemClassName}>
            <SettingsIcon className="h-4 w-4 shrink-0" />
            Configurações
          </Link>
          <Link to="/configuracoes/grupos" role="menuitem" onClick={() => setOpen(false)} className={itemClassName}>
            <UsersIcon className="h-4 w-4 shrink-0" />
            Grupos (casal/família)
            <PendingInvitesBadge />
          </Link>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false)
              logout()
            }}
            className={`${itemClassName} border-t border-zinc-200 dark:border-zinc-700`}
          >
            <LogOutIcon className="h-4 w-4 shrink-0" />
            Sair
          </button>
        </div>
      )}
    </div>
  )
}
