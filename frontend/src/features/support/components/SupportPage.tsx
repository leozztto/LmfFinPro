import { Link } from 'react-router-dom'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { LEGAL_ENTITY } from '@/features/legal/legalEntity'
import { Button } from '@/shared/ui'
import { MailIcon } from '@/shared/ui/icons'
import { Footer } from '@/shared/layout/Footer'
import { PublicPageHeader } from '@/shared/layout/PublicPageHeader'
import { WHATSAPP_HREF } from '../whatsapp'
import { usePlatformStatus } from '../hooks/usePlatformStatus'
import type { ServiceState } from '../types'

const STATE_BADGE_CLASSES: Record<ServiceState, string> = {
  OPERATIONAL: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300',
  OUTAGE: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
}

const cardClassName = 'rounded-xl border border-zinc-200 bg-zinc-50 p-5 dark:border-zinc-700 dark:bg-zinc-800'

/**
 * Página pública de suporte: canais de atendimento e situação da plataforma. Fica fora do layout do
 * app para continuar acessível a quem não consegue entrar.
 */
export function SupportPage() {
  return (
    <div className="flex min-h-dvh flex-col bg-white dark:bg-zinc-900">
      <PublicPageHeader />

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-8 px-4 py-8 text-sm leading-relaxed text-zinc-700 dark:text-zinc-300">
        <header className="space-y-2">
          <h1 className="text-2xl font-semibold text-zinc-800 dark:text-zinc-100">Suporte</h1>
          <p>Fale com a gente ou confira se a plataforma está funcionando normalmente.</p>
          <p>
            Antes de escrever, veja se a resposta está nas{' '}
            <Link to="/faq" className={authLinkClassName}>
              perguntas frequentes
            </Link>{' '}
            (é preciso estar logado).
          </p>
        </header>

        <section aria-labelledby="support-contact" className="space-y-3">
          <h2 id="support-contact" className="text-base font-semibold text-zinc-800 dark:text-zinc-100">
            Como falar com a gente
          </h2>
          <div className={`${cardClassName} grid grid-cols-1 gap-4 sm:grid-cols-2`}>
            <p className="min-w-0">
              <span className="block text-xs text-zinc-500 dark:text-zinc-400">E-mail</span>
              <a
                href={`mailto:${LEGAL_ENTITY.privacyEmail}`}
                className={`${authLinkClassName} inline-flex items-center gap-1.5 break-all`}
              >
                <MailIcon className="h-4 w-4 shrink-0" />
                {LEGAL_ENTITY.privacyEmail}
              </a>
            </p>
            <p className="min-w-0">
              <span className="block text-xs text-zinc-500 dark:text-zinc-400">Telefone e WhatsApp</span>
              <a
                href={WHATSAPP_HREF}
                target="_blank"
                rel="noopener noreferrer"
                title="Abrir conversa no WhatsApp"
                className={authLinkClassName}
              >
                {LEGAL_ENTITY.phone}
              </a>
            </p>
          </div>
          <ul className="list-disc space-y-1.5 pl-5">
            <li>Conte o que aconteceu, em qual tela e o e-mail da sua conta. Um print ajuda.</li>
            <li>Nunca envie sua senha, nem por e-mail nem por telefone: nós não pedimos.</li>
            <li>
              Pedidos sobre seus dados pessoais (LGPD) seguem a{' '}
              <Link to="/privacidade" className={authLinkClassName}>
                Política de Privacidade
              </Link>
              .
            </li>
          </ul>
        </section>

        <PlatformStatusSection />
      </main>

      <Footer />
    </div>
  )
}

function PlatformStatusSection() {
  const { data, isLoading, isError, isFetching, refetch } = usePlatformStatus()

  return (
    <section aria-labelledby="support-status" className="space-y-3">
      <h2 id="support-status" className="text-base font-semibold text-zinc-800 dark:text-zinc-100">
        Status da plataforma
      </h2>

      <div className={`${cardClassName} space-y-3`}>
        {isLoading && <p>Verificando…</p>}

        {isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-red-700 dark:bg-red-500/10 dark:text-red-300">
            Não conseguimos falar com o servidor agora. Pode ser uma instabilidade nossa ou da sua conexão: tente
            atualizar em instantes ou fale com a gente pelos canais acima.
          </p>
        )}

        {data && (
          <p role="status" className={`rounded-lg px-3 py-2 font-medium ${STATE_BADGE_CLASSES[data.status]}`}>
            {data.status === 'OPERATIONAL'
              ? 'Todos os sistemas estão operacionais.'
              : 'Estamos com uma instabilidade. Já estamos de olho.'}
          </p>
        )}

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            {data && (
              <>
                Verificado às{' '}
                {new Date(data.checkedAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}.{' '}
              </>
            )}
            Atualiza sozinho a cada minuto.
          </p>
          <Button
            type="button"
            variant="secondary"
            onClick={() => void refetch()}
            disabled={isFetching}
            className="w-full sm:w-auto"
          >
            {isFetching ? 'Verificando…' : 'Atualizar'}
          </Button>
        </div>
      </div>
    </section>
  )
}
