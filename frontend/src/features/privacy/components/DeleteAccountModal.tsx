import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, FormField, Input, Modal } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useAuth } from '@/shared/auth/AuthContext'
import { useToast } from '@/shared/toast/ToastContext'
import { useDeleteAccount, useDeletionPreview } from '../hooks/usePrivacy'
import { DELETE_CONFIRMATION_WORD, canConfirmDeletion } from '../deletionConfirmation'
import type { AccountDeletionPreview, DeletionGroup } from '../types'

interface DeleteAccountModalProps {
  open: boolean
  onClose: () => void
}

/**
 * Exclusão definitiva da conta. Mostra antes, com dados do servidor, o que será apagado e o que
 * fica; pede a senha e a palavra EXCLUIR. Quando o servidor informa um bloqueio (dono de grupo com
 * outros membros), o botão não habilita.
 */
export function DeleteAccountModal({ open, onClose }: DeleteAccountModalProps) {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const { showToast } = useToast()
  const preview = useDeletionPreview(open)
  const deleteAccount = useDeleteAccount()
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')

  const data = preview.data
  const canSubmit = canConfirmDeletion({
    password,
    confirmation,
    blocked: data ? !data.canDelete : false,
    loadingPreview: !data,
  })

  function close() {
    if (deleteAccount.isPending) return
    setPassword('')
    setConfirmation('')
    deleteAccount.reset()
    onClose()
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!canSubmit) return
    deleteAccount.mutate(password, {
      onSuccess: () => {
        logout()
        showToast('Sua conta foi excluída.', 'success')
        navigate('/login', { replace: true })
      },
    })
  }

  return (
    <Modal open={open} onClose={close} title="Excluir minha conta">
      <form onSubmit={submit} className="space-y-5">
        <p className="text-sm text-zinc-600 dark:text-zinc-300">
          A exclusão é <strong>definitiva</strong>: não há como recuperar a conta depois. Se quiser guardar uma
          cópia, baixe seus dados antes.
        </p>

        {preview.isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Verificando o que será apagado…</p>}
        {preview.isError && (
          <p role="alert" className="text-sm text-red-600">
            Não foi possível verificar o que será apagado. Tente novamente em instantes.
          </p>
        )}
        {data && <DeletionSummary data={data} />}

        <FormField label="Sua senha" htmlFor="delete-account-password">
          <Input
            id="delete-account-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </FormField>
        <FormField label={`Digite ${DELETE_CONFIRMATION_WORD} para confirmar`} htmlFor="delete-account-confirmation">
          <Input
            id="delete-account-confirmation"
            autoComplete="off"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
          />
        </FormField>

        {deleteAccount.isError && (
          <p role="alert" className="text-sm text-red-600">
            {deleteAccount.error instanceof ApiError
              ? deleteAccount.error.message
              : 'Não foi possível excluir a conta.'}
          </p>
        )}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={close} className="w-full sm:w-auto">
            Cancelar
          </Button>
          <Button type="submit" variant="danger" disabled={!canSubmit || deleteAccount.isPending} className="w-full sm:w-auto">
            {deleteAccount.isPending ? 'Excluindo…' : 'Excluir definitivamente'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function DeletionSummary({ data }: { data: AccountDeletionPreview }) {
  return (
    <div className="space-y-3 text-sm">
      {data.blockers.map((blocker) => (
        <p
          key={blocker}
          role="alert"
          className="rounded-lg bg-red-50 px-3 py-2 text-red-700 dark:bg-red-950 dark:text-red-300"
        >
          {blocker}
        </p>
      ))}

      <div className="rounded-lg border border-zinc-200 p-3 dark:border-zinc-700">
        <p className="font-medium text-zinc-800 dark:text-zinc-100">Será apagado</p>
        <ul className="mt-1 list-disc space-y-1 pl-5 text-zinc-600 dark:text-zinc-300">
          <li>Seus dados cadastrais, sessões, preferências e a foto de perfil.</li>
          {data.deletedGroups.map((group) => (
            <li key={group.id}>{describeDeletedGroup(group)}</li>
          ))}
          {data.attachmentCount > 0 && (
            <li>
              {data.attachmentCount} {data.attachmentCount === 1 ? 'anexo' : 'anexos'} de transações.
            </li>
          )}
        </ul>
      </div>

      {data.leftGroups.length > 0 && (
        <div className="rounded-lg border border-zinc-200 p-3 dark:border-zinc-700">
          <p className="font-medium text-zinc-800 dark:text-zinc-100">Você sai destes grupos</p>
          <ul className="mt-1 list-disc space-y-1 pl-5 text-zinc-600 dark:text-zinc-300">
            {data.leftGroups.map((group) => (
              <li key={group.id}>{describeLeftGroup(group)}</li>
            ))}
          </ul>
          <p className="mt-2 text-xs text-zinc-500 dark:text-zinc-400">
            Para levar contas embora, devolva-as ao seu espaço pessoal antes de excluir.
          </p>
        </div>
      )}
    </div>
  )
}

function describeDeletedGroup(group: DeletionGroup): string {
  return group.type === 'PERSONAL'
    ? 'Todo o seu espaço pessoal: contas, lançamentos, categorias, metas e demais registros.'
    : `O grupo "${group.name}" e tudo o que há nele (só você participa).`
}

function describeLeftGroup(group: DeletionGroup): string {
  const brought = group.accountsBroughtByYou
  if (brought === 0) return `"${group.name}": os dados continuam com o grupo, sem o seu nome.`
  const accounts = brought === 1 ? '1 conta que você trouxe fica' : `${brought} contas que você trouxe ficam`
  return `"${group.name}": ${accounts} no grupo, sem o seu nome.`
}
