import type { ReactNode } from 'react'
import { GitHubIcon, GlobeIcon, LinkedInIcon, MailIcon } from '@/shared/ui/icons'
import { LEGAL_ENTITY } from '@/features/legal/legalEntity'

const EMAIL = LEGAL_ENTITY.privacyEmail
const GITHUB_URL = 'https://github.com/leozztto'
const LINKEDIN_URL = 'https://www.linkedin.com/in/leandro-mf'
const PORTFOLIO_URL = 'https://portfolio-leandromf.vercel.app/'

/** Alvo de toque de 40px (acima do mínimo recomendado para celular) mesmo com ícone pequeno. */
const iconLinkClassName =
  'flex h-10 w-10 items-center justify-center rounded-lg text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-[#1ea883] active:text-[#1ea883] dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-[#2ad6a5] dark:active:text-[#2ad6a5]'

function ExternalIconLink({ href, label, children }: { href: string; label: string; children: ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer" aria-label={label} title={label} className={iconLinkClassName}>
      {children}
    </a>
  )
}

/** Rodapé do app: no celular o direito autoral vem em cima, os contatos embaixo e tudo fica centralizado; de `sm` em diante o
 *  direito autoral fica à esquerda e os contatos à direita. */
export function Footer() {
  return (
    <footer className="border-t border-zinc-200 px-4 py-4 dark:border-zinc-800 sm:px-6">
      <div className="mx-auto flex max-w-5xl flex-col items-center gap-2 text-center sm:flex-row sm:justify-between sm:text-left">
        <p className="text-xs text-balance text-zinc-400 dark:text-zinc-500">
          © 2026 {LEGAL_ENTITY.name}. Todos os direitos reservados.
        </p>
        <nav aria-label="Contato e redes" className="flex items-center justify-center gap-1">
          <a href={`mailto:${EMAIL}`} aria-label={`Enviar e-mail para ${EMAIL}`} title={EMAIL} className={iconLinkClassName}>
            <MailIcon />
          </a>
          <ExternalIconLink href={GITHUB_URL} label="Perfil no GitHub">
            <GitHubIcon />
          </ExternalIconLink>
          <ExternalIconLink href={LINKEDIN_URL} label="Perfil no LinkedIn">
            <LinkedInIcon />
          </ExternalIconLink>
          <ExternalIconLink href={PORTFOLIO_URL} label="Portfólio">
            <GlobeIcon />
          </ExternalIconLink>
        </nav>
      </div>
    </footer>
  )
}
