import { Card } from '@/shared/ui'
import { ChangePasswordForm } from './ChangePasswordForm'

export function PasswordPage() {
  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Alterar senha</h3>
      <p className="mb-6 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Ao trocar a senha, você continua conectado aqui, mas as outras sessões abertas (outros navegadores e
        aparelhos) são encerradas.
      </p>
      <ChangePasswordForm />
    </Card>
  )
}
