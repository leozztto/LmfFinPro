import { Button, IconButton } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useAuth } from '@/shared/auth/AuthContext'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useHouseholdMembers, useRemoveMember, useTransferOwnership } from '../hooks/useHouseholds'
import { HOUSEHOLD_ROLE_LABELS, type HouseholdMember } from '../types'

interface MembersSectionProps {
  householdId: number
  householdName: string
  /** Quem consulta é o dono do grupo: só ele remove membros e transfere a posse. */
  isOwner: boolean
}

export function MembersSection({ householdId, householdName, isOwner }: MembersSectionProps) {
  const { session } = useAuth()
  const { data: members, isLoading, isError } = useHouseholdMembers(householdId)
  const removeMember = useRemoveMember(householdId)
  const transferOwnership = useTransferOwnership(householdId)
  const { showToast } = useToast()
  const confirm = useConfirm()

  async function handleRemove(member: HouseholdMember) {
    const confirmed = await confirm({
      title: 'Remover do grupo',
      message: `Remover ${member.name} de "${householdName}"? Ela deixa de ver os dados do grupo; o que ela lançou fica no grupo.`,
      confirmLabel: 'Remover',
    })
    if (!confirmed) return
    removeMember.mutate(member.userId, {
      onSuccess: () => showToast(`${member.name} saiu do grupo.`, 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a pessoa.'),
    })
  }

  async function handleTransfer(member: HouseholdMember) {
    const confirmed = await confirm({
      title: 'Transferir a posse',
      message: `Passar a posse de "${householdName}" para ${member.name}? Você continua no grupo como membro e deixa de poder convidar ou remover pessoas.`,
      confirmLabel: 'Transferir',
      variant: 'brand',
    })
    if (!confirmed) return
    transferOwnership.mutate(member.userId, {
      onSuccess: () => showToast(`${member.name} agora é o dono do grupo.`, 'success'),
      onError: (error) =>
        showToast(error instanceof ApiError ? error.message : 'Não foi possível transferir a posse.'),
    })
  }

  return (
    <section className="space-y-3">
      <h4 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Membros</h4>
      {isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando membros...</p>}
      {isError && <p className="text-sm text-red-600">Não foi possível carregar os membros.</p>}
      {members && (
        <ul className="space-y-2">
          {members.map((member) => {
            const isMe = member.userId === session?.userId
            return (
              <li
                key={member.userId}
                className="flex min-w-0 flex-col gap-2 rounded-lg border border-zinc-200 bg-white px-3 py-2 dark:border-zinc-700 dark:bg-zinc-900 sm:flex-row sm:items-center sm:gap-3"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-zinc-800 dark:text-zinc-100" title={member.name}>
                    {member.name}
                    {isMe && <span className="ml-1 font-normal text-zinc-500 dark:text-zinc-400">(você)</span>}
                  </p>
                  <p className="truncate text-xs text-zinc-500 dark:text-zinc-400" title={member.email}>
                    {member.email}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      member.role === 'OWNER'
                        ? 'bg-[#2ad6a5]/15 text-[#1ea883] dark:text-[#2ad6a5]'
                        : 'bg-zinc-100 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300'
                    }`}
                  >
                    {HOUSEHOLD_ROLE_LABELS[member.role]}
                  </span>
                  {isOwner && !isMe && (
                    <>
                      <Button
                        type="button"
                        variant="secondary"
                        className="px-2 py-1 text-xs"
                        onClick={() => handleTransfer(member)}
                        disabled={transferOwnership.isPending}
                      >
                        Tornar dono
                      </Button>
                      <IconButton
                        icon={TrashIcon}
                        label={`Remover ${member.name} do grupo`}
                        onClick={() => handleRemove(member)}
                        disabled={removeMember.isPending}
                      />
                    </>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
