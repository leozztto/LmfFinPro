import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { PublicPageHeader } from '@/shared/layout/PublicPageHeader'
import { formatDateOnlyBr } from '@/shared/format/date'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { LEGAL_DRAFT } from '../legalEntity'
import { useLegalVersions } from '../hooks/useLegal'

interface LegalDocumentLayoutProps {
  title: string
  /** Qual versão mostrar: a dos Termos ou a da Política. */
  document: 'terms' | 'privacy'
  /** Resumo em linguagem simples, mostrado antes do texto completo. */
  summary: string
  children: ReactNode
}

/**
 * Casca das páginas públicas /termos e /privacidade. Não depende do layout do app: a pessoa precisa
 * conseguir ler os documentos antes de criar a conta. A versão mostrada vem da API, a mesma que o
 * cadastro e o aceite exigem.
 */
export function LegalDocumentLayout({ title, document, summary, children }: LegalDocumentLayoutProps) {
  const { search } = useLocation()
  const versions = useLegalVersions()
  const version = document === 'terms' ? versions.data?.termsVersion : versions.data?.privacyVersion

  return (
    <div className="min-h-dvh bg-white dark:bg-zinc-900">
      <PublicPageHeader />

      <main className="mx-auto w-full max-w-5xl space-y-8 px-4 py-6 sm:px-6 sm:py-10 text-justify text-sm leading-relaxed text-zinc-700 hyphens-auto dark:text-zinc-300">
        <header className="space-y-3">
          <h1 className="text-2xl font-semibold text-zinc-800 dark:text-zinc-100">{title}</h1>
          {version && (
            <p className="text-xs text-zinc-500 dark:text-zinc-400">Em vigor desde {formatDateOnlyBr(version)}</p>
          )}
          {LEGAL_DRAFT && (
            <p
              role="note"
              className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-200"
            >
              Minuta em revisão jurídica. Os trechos entre colchetes ainda precisam ser preenchidos pela empresa
              antes da publicação.
            </p>
          )}
          <p className="rounded-lg bg-zinc-100 px-3 py-3 dark:bg-zinc-800">
            <strong className="text-zinc-800 dark:text-zinc-100">Resumo: </strong>
            {summary}
          </p>
        </header>

        {children}

        <nav aria-label="Outros documentos" className="border-t border-zinc-200 pt-4 text-xs dark:border-zinc-800">
          <Link to={`/termos${search}`} className={authLinkClassName}>
            Termos de Uso
          </Link>
          <span className="mx-2 text-zinc-400">·</span>
          <Link to={`/privacidade${search}`} className={authLinkClassName}>
            Política de Privacidade
          </Link>
        </nav>
      </main>
    </div>
  )
}

export function LegalSection({ title, id, children }: { title: string; id?: string; children: ReactNode }) {
  return (
    <section id={id} className="scroll-mt-4 space-y-3">
      <h2 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">{title}</h2>
      {children}
    </section>
  )
}

export function LegalList({ children }: { children: ReactNode }) {
  return <ul className="list-disc space-y-1.5 pl-5">{children}</ul>
}
