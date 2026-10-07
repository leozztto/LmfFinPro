import { pendingInvitesLabel } from '../badgeCount'
import { useReceivedInvites } from '../hooks/useHouseholds'

/**
 * Bolinha com o número de convites de grupo esperando resposta, ao lado do item "Grupos" do menu.
 * É só uma pista discreta: aceitar e recusar fica em Configurações → Grupos. Não aparece sem convite.
 */
export function PendingInvitesBadge() {
  const { data: invites } = useReceivedInvites()
  const count = invites?.length ?? 0
  if (count === 0) return null

  return (
    <span
      role="status"
      aria-label={pendingInvitesLabel(count)}
      className="ml-1 inline-flex min-w-[1.25rem] shrink-0 items-center justify-center rounded-full bg-[#2ad6a5] px-1.5 text-xs font-semibold leading-5 text-zinc-900"
    >
      {count}
    </span>
  )
}
