import { useState } from 'react'
import { Button, Card } from '@/shared/ui'
import { UsersIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { useLeaveHousehold } from '../hooks/useHouseholds'
import { HOUSEHOLD_ROLE_LABELS, type Household } from '../types'
import { InvitesSection } from './InvitesSection'
import { MembersSection } from './MembersSection'
import { ShareAccountsModal } from './ShareAccountsModal'

/** Um grupo compartilhado: ver os dados dele, compartilhar contas, gerir pessoas e convites. */
export function HouseholdCard({ household }: { household: Household }) {
  const isOwner = household.role === 'OWNER'
  const { active, isShared, switchTo } = useHousehold()
  const leave = useLeaveHousehold(household.id)
  const { showToast } = useToast()
  const confirm = useConfirm()
  const [sharing, setSharing] = useState(false)
  const viewing = isShared && active?.id === household.id

  async function handleLeave() {
    const confirmed = await confirm({
      title: 'Sair do grupo',
      message: `Sair de "${household.name}"? Você deixa de ver os dados do grupo. O que você lançou nele fica lá, e os seus dados pessoais não mudam.`,
      confirmLabel: 'Sair do grupo',
    })
    if (!confirmed) return
    leave.mutate(undefined, {
      onSuccess: () => {
        // Se era o grupo em exibição, volta para os dados pessoais (o provedor também conferiria).
        if (viewing) switchTo(null)
        showToast('Você saiu do grupo.', 'success')
      },
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível sair do grupo.'),
    })
  }

  return (
    <Card className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 items-start gap-3">
          <UsersIcon className="mt-0.5 h-5 w-5 shrink-0 text-[#1ea883] dark:text-[#2ad6a5]" />
          <div className="min-w-0">
            <h3 className="truncate text-base font-semibold text-zinc-800 dark:text-zinc-100" title={household.name}>
              {household.name}
            </h3>
            <p className="text-xs text-zinc-500 dark:text-zinc-400">
              Você é {HOUSEHOLD_ROLE_LABELS[household.role].toLowerCase()} deste grupo
              {viewing && ' · em exibição agora'}
            </p>
          </div>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:shrink-0">
          {!viewing && (
            <Button type="button" variant="secondary" onClick={() => switchTo(household.id)}>
              Ver dados do grupo
            </Button>
          )}
          <Button type="button" variant="brand" onClick={() => setSharing(true)}>
            Compartilhar contas
          </Button>
        </div>
      </div>

      <MembersSection householdId={household.id} householdName={household.name} isOwner={isOwner} />

      {isOwner ? (
        <InvitesSection householdId={household.id} />
      ) : (
        <div className="border-t border-zinc-200 pt-4 dark:border-zinc-700">
          <p className="mb-3 text-sm text-zinc-500 dark:text-zinc-400">
            Só o dono do grupo convida e remove pessoas.
          </p>
          <Button type="button" variant="danger" onClick={handleLeave} disabled={leave.isPending}>
            {leave.isPending ? 'Saindo...' : 'Sair do grupo'}
          </Button>
        </div>
      )}

      {sharing && <ShareAccountsModal household={household} onClose={() => setSharing(false)} />}
    </Card>
  )
}
