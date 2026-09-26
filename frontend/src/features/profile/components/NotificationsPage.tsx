import { Card } from '@/shared/ui'
import { useNotificationPreferences } from '../hooks/useNotificationPreferences'
import { NotificationPreferencesForm } from './NotificationPreferencesForm'

export function NotificationsPage() {
  const { data: preferences, isLoading, isError } = useNotificationPreferences()

  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Notificações por e-mail</h3>
      <p className="mb-6 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Todo dia de manhã, se houver algo novo, você recebe um único e-mail com os alertas escolhidos abaixo. Cada
        aviso chega uma vez só.
      </p>
      {isLoading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>}
      {isError && <p className="text-sm text-red-600">Não foi possível carregar as preferências.</p>}
      {preferences && <NotificationPreferencesForm preferences={preferences} />}
    </Card>
  )
}
