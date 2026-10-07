import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Button } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useAuth } from '@/shared/auth/AuthContext'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { useAcceptConsent, useConsentStatus } from '../hooks/useLegal'
import { isConsentScreenExempt } from '../consentGate'

/**
 * Trava o app, depois do login, enquanto a pessoa não aceitar as versões vigentes dos Termos e da
 * Política. Quem já tinha conta antes dos documentos existirem, ou quando eles mudam, passa por
 * aqui. A tela de Privacidade e dados continua liberada: sem aceitar, ainda dá para baixar os dados
 * e excluir a conta (LGPD). Se a consulta falhar, o app abre normalmente: o servidor registra o
 * aceite, mas não deixamos a pessoa trancada por uma falha de rede.
 */
export function ConsentGate({ children }: { children: ReactNode }) {
  const status = useConsentStatus()
  const { pathname } = useLocation()

  if (status.isLoading) {
    return (
      <div role="status" className="flex min-h-screen items-center justify-center text-sm text-zinc-500 dark:text-zinc-400">
        Carregando…
      </div>
    )
  }
  if (!status.data?.pending || isConsentScreenExempt(pathname)) {
    return <>{children}</>
  }
  return <ConsentRequiredScreen />
}

function ConsentRequiredScreen() {
  const { logout } = useAuth()
  const status = useConsentStatus()
  const accept = useAcceptConsent()
  const data = status.data
  if (!data) return null

  const isUpdate = data.terms.acceptedVersion !== null || data.privacy.acceptedVersion !== null

  return (
    <div className="flex min-h-dvh flex-col bg-white dark:bg-zinc-900">
      <div className="flex justify-end px-4 pt-4">
        <ThemeToggle />
      </div>
      <div className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-lg rounded-xl border border-zinc-200 bg-zinc-50 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-800 sm:p-8">
          <h1 className="text-xl font-semibold text-zinc-800 dark:text-zinc-100">
            {isUpdate ? 'Atualizamos nossos documentos' : 'Antes de continuar'}
          </h1>
          <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-300">
            {isUpdate
              ? 'Os Termos de Uso ou a Política de Privacidade mudaram. Leia as versões novas e confirme para continuar usando o FinPro.'
              : 'Para continuar usando o FinPro, leia e aceite os Termos de Uso e a Política de Privacidade.'}
          </p>

          <ul className="mt-4 space-y-2 text-sm">
            <li>
              <Link to="/termos" target="_blank" rel="noopener noreferrer" className={authLinkClassName}>
                Termos de Uso
              </Link>
              <span className="text-zinc-500 dark:text-zinc-400"> (versão {data.terms.currentVersion})</span>
            </li>
            <li>
              <Link to="/privacidade" target="_blank" rel="noopener noreferrer" className={authLinkClassName}>
                Política de Privacidade
              </Link>
              <span className="text-zinc-500 dark:text-zinc-400"> (versão {data.privacy.currentVersion})</span>
            </li>
          </ul>

          {accept.isError && (
            <p role="alert" className="mt-4 text-sm text-red-600">
              {accept.error instanceof ApiError ? accept.error.message : 'Não foi possível registrar o aceite.'}
            </p>
          )}

          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-between">
            <Button type="button" variant="secondary" onClick={logout} className="w-full sm:w-auto">
              Sair
            </Button>
            <Button
              type="button"
              variant="brand"
              disabled={accept.isPending}
              className="w-full sm:w-auto"
              onClick={() =>
                accept.mutate({ termsVersion: data.terms.currentVersion, privacyVersion: data.privacy.currentVersion })
              }
            >
              {accept.isPending ? 'Registrando…' : 'Li e aceito'}
            </Button>
          </div>

          <p className="mt-6 border-t border-zinc-200 pt-4 text-xs text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">
            Não quer aceitar? Você pode{' '}
            <Link to="/configuracoes/privacidade" className={authLinkClassName}>
              baixar seus dados ou excluir a conta
            </Link>
            .
          </p>
        </div>
      </div>
    </div>
  )
}
