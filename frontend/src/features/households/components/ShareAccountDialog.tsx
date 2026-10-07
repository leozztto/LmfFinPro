import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, FormField, Modal, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { useShareAccountsToGroup } from '../hooks/useShareAccounts'

interface ShareAccountDialogProps {
  accountId: number
  accountName: string
  onClose: () => void
}

/**
 * Compartilha uma conta do espaço pessoal com um grupo, a partir da tela de Contas. Monte só quando
 * for abrir: a escolha do grupo e o erro começam do zero a cada vez.
 */
export function ShareAccountDialog({ accountId, accountName, onClose }: ShareAccountDialogProps) {
  const { sharedHouseholds } = useHousehold()
  const shareAccount = useShareAccountsToGroup()
  const { showToast } = useToast()
  const [householdId, setHouseholdId] = useState<number | null>(sharedHouseholds[0]?.id ?? null)
  const selected = sharedHouseholds.find((household) => household.id === householdId)

  function handleShare() {
    if (householdId === null) return
    shareAccount.mutate(
      { householdId, accountIds: [accountId] },
      {
        onSuccess: (result) => {
          const transactions = `${result.transactions} ${result.transactions === 1 ? 'transação' : 'transações'}`
          showToast(`"${accountName}" e ${transactions} agora são do grupo "${selected?.name}".`, 'success')
          onClose()
        },
      },
    )
  }

  return (
    <Modal open onClose={onClose} title={`Compartilhar "${accountName}"`}>
      {sharedHouseholds.length === 0 ? (
        <div className="space-y-4">
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            Você ainda não participa de nenhum grupo. Crie um (ou aceite um convite) em{' '}
            <Link to="/configuracoes/grupos" onClick={onClose} className="font-medium text-blue-600 hover:underline dark:text-blue-400">
              Configurações → Grupos
            </Link>{' '}
            para poder compartilhar contas.
          </p>
          <div className="flex justify-end">
            <Button type="button" variant="secondary" onClick={onClose}>
              Fechar
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {sharedHouseholds.length > 1 && (
            <FormField label="Compartilhar com o grupo" htmlFor="share-account-group">
              <Select
                id="share-account-group"
                value={householdId ?? ''}
                onChange={(event) => setHouseholdId(Number(event.target.value))}
              >
                {sharedHouseholds.map((household) => (
                  <option key={household.id} value={household.id}>
                    {household.name}
                  </option>
                ))}
              </Select>
            </FormField>
          )}
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            A conta <strong>{accountName}</strong> e todo o histórico dela (transações, importações, recorrências e
            anexos) passam a ser do grupo <strong>{selected?.name}</strong> e saem dos seus dados pessoais. Todos os
            membros poderão ver e editar a conta. Você pode descompartilhá-la depois, e ela volta para os seus dados com
            o histórico.
          </p>
          {shareAccount.isError && (
            <p role="alert" className="text-sm text-red-600">
              {shareAccount.error instanceof ApiError
                ? shareAccount.error.message
                : 'Não foi possível compartilhar a conta.'}
            </p>
          )}
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="secondary" className="w-full sm:w-auto" onClick={onClose}>
              Cancelar
            </Button>
            <Button
              type="button"
              variant="brand"
              className="w-full sm:w-auto"
              onClick={handleShare}
              disabled={householdId === null || shareAccount.isPending}
            >
              {shareAccount.isPending ? 'Compartilhando...' : 'Compartilhar'}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
