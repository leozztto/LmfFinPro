import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useAuth } from '@/shared/auth/AuthContext'
import { useToast } from '@/shared/toast/ToastContext'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { AuthPageShell, authLinkClassName } from '@/features/auth/components/AuthPageShell'
import type { LoginLocationState } from '@/features/auth/components/LoginPage'
import { useAcceptInvite } from '../hooks/useHouseholds'

/**
 * Destino do link do e-mail de convite (/convite?token=...). Quem já está logado aceita direto, com
 * os dados pessoais intactos; quem não está entra ou cria a conta e volta aqui (ou, no cadastro, já
 * entra no grupo ao criar a conta).
 */
export function InvitePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const { session, restoring } = useAuth()
  const { switchTo } = useHousehold()
  const { showToast } = useToast()
  const acceptInvite = useAcceptInvite()

  async function handleAccept() {
    try {
      const household = await acceptInvite.mutateAsync(token)
      showToast(`Você agora participa do grupo "${household.name}".`, 'success')
      switchTo(household.id)
      navigate('/', { replace: true })
    } catch {
      // A mensagem do backend (convite inválido ou expirado) aparece abaixo, via acceptInvite.error.
    }
  }

  const loginState: LoginLocationState = { redirectTo: `/convite?token=${encodeURIComponent(token)}` }

  return (
    <AuthPageShell>
      <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Convite para um grupo</h2>

      {restoring ? (
        <p role="status" className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
          Carregando…
        </p>
      ) : !token ? (
        <p className="mt-4 text-sm text-zinc-600 dark:text-zinc-300">
          Este link de convite está incompleto. Abra o link do e-mail novamente ou peça um novo convite.
        </p>
      ) : session ? (
        <div className="mt-4 space-y-4">
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            Você foi convidado para compartilhar contas, transações e relatórios num grupo. Seus dados pessoais
            continuam só seus; depois, se quiser, escolha quais contas levar para o grupo.
          </p>
          {acceptInvite.isError && (
            <p role="alert" className="text-sm text-red-600">
              {acceptInvite.error instanceof ApiError ? acceptInvite.error.message : 'Não foi possível aceitar o convite.'}
            </p>
          )}
          <Button
            type="button"
            variant="brand"
            className="w-full"
            onClick={handleAccept}
            disabled={acceptInvite.isPending}
          >
            {acceptInvite.isPending ? 'Entrando no grupo...' : 'Aceitar convite'}
          </Button>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            <Link to="/" className={authLinkClassName}>
              Agora não
            </Link>
          </p>
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          <p className="text-sm text-zinc-600 dark:text-zinc-300">
            Você foi convidado para compartilhar contas, transações e relatórios num grupo do FinPro. Entre na sua conta
            ou crie uma para aceitar.
          </p>
          <Link to="/login" state={loginState} className="block">
            <Button type="button" variant="brand" className="w-full">
              Entrar
            </Button>
          </Link>
          <Link to={`/registro?convite=${encodeURIComponent(token)}`} className="block">
            <Button type="button" variant="secondary" className="w-full">
              Criar conta
            </Button>
          </Link>
        </div>
      )}
    </AuthPageShell>
  )
}
