import { Button, Card } from '@/shared/ui'
import { UsersIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { useAcceptReceivedInvite, useDeclineReceivedInvite, useReceivedInvites } from '../hooks/useHouseholds'
import type { ReceivedInvite } from '../types'

/**
 * Convites para grupos (casal/família) enviados ao e-mail desta conta: dá para aceitar ou recusar
 * aqui mesmo, sem depender do link do e-mail. Some quando não há convite pendente.
 */
export function ReceivedInvitesCard() {
  const { data: invites } = useReceivedInvites()
  const accept = useAcceptReceivedInvite()
  const decline = useDeclineReceivedInvite()
  const { showToast } = useToast()
  const confirm = useConfirm()
  const { switchTo } = useHousehold()

  if (!invites || invites.length === 0) return null

  async function handleAccept(invite: ReceivedInvite) {
    try {
      await accept.mutateAsync(invite.id)
      // A lista de grupos já foi recarregada: passa a mostrar os dados do grupo, e o painel e as demais
      // telas buscam os dados dele na hora (a troca descarta o cache do grupo anterior).
      switchTo(invite.householdId)
      showToast(
        `Você agora participa do grupo "${invite.householdName}". Os dados dele já estão em exibição; para voltar aos seus, use o seletor no topo.`,
        'success',
      )
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível aceitar o convite.')
    }
  }

  async function handleDecline(invite: ReceivedInvite) {
    const confirmed = await confirm({
      title: 'Recusar convite',
      message: `Recusar o convite de ${invite.inviterName} para o grupo "${invite.householdName}"? O link do e-mail deixa de funcionar e, para entrar, será preciso um novo convite.`,
      confirmLabel: 'Recusar',
    })
    if (!confirmed) return
    try {
      await decline.mutateAsync(invite.id)
      showToast('Convite recusado.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível recusar o convite.')
    }
  }

  const busy = accept.isPending || decline.isPending

  return (
    <Card className="border-[#2ad6a5]">
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Convites recebidos</h3>
      <p className="mb-4 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Convites enviados para o e-mail da sua conta. Ao aceitar, você passa a ver e editar os dados do grupo junto com
        os demais membros; seus dados pessoais continuam só seus.
      </p>
      <ul className="space-y-3">
        {invites.map((invite) => (
          <li
            key={invite.id}
            className="flex min-w-0 flex-col gap-3 rounded-lg border border-zinc-200 bg-white p-3 dark:border-zinc-700 dark:bg-zinc-900 sm:flex-row sm:items-center"
          >
            <div className="flex min-w-0 flex-1 items-start gap-3">
              <UsersIcon className="mt-0.5 h-5 w-5 shrink-0 text-[#1ea883] dark:text-[#2ad6a5]" />
              <div className="min-w-0">
                <p className="text-sm font-medium text-zinc-800 dark:text-zinc-100">
                  {invite.inviterName} convidou você para o grupo{' '}
                  <span className="break-words font-semibold">"{invite.householdName}"</span>
                </p>
                <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
                  Vale até {new Date(invite.expiresAt).toLocaleDateString('pt-BR')}
                </p>
              </div>
            </div>
            <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
              <Button type="button" variant="brand" onClick={() => handleAccept(invite)} disabled={busy}>
                Aceitar
              </Button>
              <Button type="button" variant="secondary" onClick={() => handleDecline(invite)} disabled={busy}>
                Recusar
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  )
}
