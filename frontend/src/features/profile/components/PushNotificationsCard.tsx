import { Button, Card } from '@/shared/ui'
import { usePushNotifications } from '../hooks/usePushNotifications'

const MESSAGES = {
  unsupported: 'Este navegador não oferece notificações push.',
  'needs-install':
    'No iPhone e no iPad, instale o FinPro na Tela de Início (Compartilhar > Adicionar à Tela de Início) e abra por lá para ativar as notificações.',
  denied: 'As notificações estão bloqueadas neste navegador. Libere nas configurações do site para poder ativá-las.',
} as const

export function PushNotificationsCard() {
  const { status, busy, error, enable, disable } = usePushNotifications()

  // Servidor sem push configurado, ou ainda descobrindo o estado: nada a mostrar.
  if (status === 'disabled' || status === 'loading') return null

  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Notificações neste aparelho</h3>
      <p className="mb-4 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Receba o mesmo resumo como notificação no celular ou no computador, além do e-mail. Vale para os alertas
        escolhidos acima e é por aparelho: ative em cada um onde quiser ser avisado.
      </p>

      {status === 'on' || status === 'off' ? (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-zinc-700 dark:text-zinc-200">
            {status === 'on' ? 'Ativadas neste aparelho.' : 'Desativadas neste aparelho.'}
          </p>
          <Button
            type="button"
            variant={status === 'on' ? 'secondary' : 'brand'}
            className="w-full sm:w-auto"
            disabled={busy}
            onClick={status === 'on' ? disable : enable}
          >
            {busy ? 'Aguarde...' : status === 'on' ? 'Desativar' : 'Ativar notificações'}
          </Button>
        </div>
      ) : (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">{MESSAGES[status]}</p>
      )}

      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
    </Card>
  )
}
