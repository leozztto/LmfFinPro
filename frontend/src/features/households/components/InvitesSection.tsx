import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, IconButton, Input } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useHouseholdInvites, useInviteMember, useRevokeInvite } from '../hooks/useHouseholds'
import { inviteSchema, type InviteFormValues } from '../schemas'

function formatExpiry(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleDateString('pt-BR')
}

/** Só o dono convida: o e-mail leva um link, válido por alguns dias e de uso único. */
export function InvitesSection({ householdId }: { householdId: number }) {
  const { data: invites, isLoading } = useHouseholdInvites(householdId, true)
  const invite = useInviteMember(householdId)
  const revoke = useRevokeInvite(householdId)
  const { showToast } = useToast()
  const confirm = useConfirm()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InviteFormValues>({
    resolver: zodResolver(inviteSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { email: '' },
  })

  async function onSubmit(values: InviteFormValues) {
    try {
      await invite.mutateAsync(values.email)
      reset()
      showToast(`Convite enviado para ${values.email}.`, 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível enviar o convite.')
    }
  }

  async function handleRevoke(inviteId: number, email: string) {
    const confirmed = await confirm({
      title: 'Cancelar convite',
      message: `Cancelar o convite enviado para ${email}? O link deixa de funcionar.`,
      confirmLabel: 'Cancelar convite',
    })
    if (!confirmed) return
    revoke.mutate(inviteId, {
      onSuccess: () => showToast('Convite cancelado.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível cancelar o convite.'),
    })
  }

  return (
    <section className="space-y-3">
      <h4 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Convidar</h4>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3 sm:flex-row sm:items-start" noValidate>
        <div className="min-w-0 flex-1">
          <FormField
            label="E-mail da pessoa"
            htmlFor={`invite-email-${householdId}`}
            error={errors.email?.message}
            hint="Quem abrir o link entra no grupo, mesmo com outro e-mail. A pessoa pode já ter conta ou criar uma na hora."
          >
            <Input
              id={`invite-email-${householdId}`}
              type="email"
              autoComplete="off"
              placeholder="nome@exemplo.com"
              {...register('email')}
            />
          </FormField>
        </div>
        <Button type="submit" className="w-full sm:mt-6 sm:w-auto" disabled={invite.isPending}>
          {invite.isPending ? 'Enviando...' : 'Enviar convite'}
        </Button>
      </form>

      {isLoading ? (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando convites...</p>
      ) : (
        invites &&
        invites.length > 0 && (
          <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">
              Convites pendentes
            </p>
            <ul className="space-y-2">
              {invites.map((pending) => (
                <li
                  key={pending.id}
                  className="flex min-w-0 items-center gap-3 rounded-lg border border-zinc-200 bg-white px-3 py-2 dark:border-zinc-700 dark:bg-zinc-900"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm text-zinc-800 dark:text-zinc-100" title={pending.email}>
                      {pending.email}
                    </p>
                    <p className="text-xs text-zinc-500 dark:text-zinc-400">Vale até {formatExpiry(pending.expiresAt)}</p>
                  </div>
                  <IconButton
                    icon={TrashIcon}
                    label={`Cancelar convite de ${pending.email}`}
                    onClick={() => handleRevoke(pending.id, pending.email)}
                    disabled={revoke.isPending}
                  />
                </li>
              ))}
            </ul>
          </div>
        )
      )}
    </section>
  )
}
