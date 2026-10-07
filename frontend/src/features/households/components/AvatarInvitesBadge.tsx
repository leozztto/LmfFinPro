import { formatBadgeCount, pendingInvitesLabel } from '../badgeCount'

/**
 * Bolinha com o número de convites de grupo esperando resposta, no canto da foto do usuário: dá para
 * ver de qualquer tela que há algo a responder em Configurações → Grupos. Não aparece sem convite.
 * Decorativa para leitores de tela: o botão da foto já anuncia a contagem no próprio rótulo.
 */
export function AvatarInvitesBadge({ count }: { count: number }) {
  if (count <= 0) return null

  return (
    <span
      aria-hidden="true"
      title={pendingInvitesLabel(count)}
      className="pointer-events-none absolute -right-1 -top-1 inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-[#2ad6a5] px-1 text-[0.7rem] font-semibold leading-none text-zinc-900 ring-2 ring-white dark:ring-zinc-900"
    >
      {formatBadgeCount(count)}
    </span>
  )
}
