import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
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
  const versions = useLegalVersions()
  const version = document === 'terms' ? versions.data?.termsVersion : versions.data?.privacyVersion

  return (
    <div className="min-h-dvh bg-white dark:bg-zinc-900">
      <div className="mx-auto flex max-w-3xl items-center justify-between gap-3 px-4 pt-4">
        <Link to="/" className={`${authLinkClassName} text-sm`}>
          ← Voltar ao FinPro
        </Link>
        <ThemeToggle />
      </div>

      <main className="mx-auto max-w-3xl space-y-8 px-4 py-8 text-sm leading-relaxed text-zinc-700 dark:text-zinc-300">
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
          <Link to="/termos" className={authLinkClassName}>
            Termos de Uso
          </Link>
          <span className="mx-2 text-zinc-400">·</span>
          <Link to="/privacidade" className={authLinkClassName}>
            Política de Privacidade
          </Link>
        </nav>
      </main>
    </div>
  )
}

export function LegalSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">{title}</h2>
      {children}
    </section>
  )
}

export function LegalList({ children }: { children: ReactNode }) {
  return <ul className="list-disc space-y-1.5 pl-5">{children}</ul>
}
