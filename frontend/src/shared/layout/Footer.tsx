import { GitHubIcon, GlobeIcon, LinkedInIcon, MailIcon } from '@/shared/ui/icons'

const EMAIL = 'leozztto@gmail.com'
const GITHUB_URL = 'https://github.com/leozztto'
const LINKEDIN_URL = 'https://www.linkedin.com/in/leandro-mf'
const PORTFOLIO_URL = 'https://portfolio-leandromf.vercel.app/'

const iconLinkClassName =
  'text-zinc-500 transition-colors hover:text-[#2ad6a5] active:text-[#2ad6a5] dark:text-zinc-400 dark:hover:text-[#2ad6a5] dark:active:text-[#2ad6a5]'

export function Footer() {
  return (
    <footer className="border-t border-zinc-200 px-6 py-6 dark:border-zinc-800">
      <div className="mx-auto flex max-w-5xl flex-col items-center gap-4 text-center sm:flex-row sm:items-center sm:justify-between sm:text-left">
        <p className="text-xs text-zinc-400 dark:text-zinc-500">© 2026 Lezzotto Tech. Todos os direitos reservados.</p>
        <div className="flex items-center gap-4">
          <a href={`mailto:${EMAIL}`} aria-label={`Enviar e-mail para ${EMAIL}`} title={EMAIL} className={iconLinkClassName}>
            <MailIcon />
          </a>
          <a
            href={GITHUB_URL}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Perfil no GitHub"
            title="GitHub"
            className={iconLinkClassName}
          >
            <GitHubIcon />
          </a>
          {LINKEDIN_URL && (
            <a
              href={LINKEDIN_URL}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Perfil no LinkedIn"
              title="LinkedIn"
              className={iconLinkClassName}
            >
              <LinkedInIcon />
            </a>
          )}
          <a
            href={PORTFOLIO_URL}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Portfólio"
            title="Portfólio"
            className={iconLinkClassName}
          >
            <GlobeIcon />
          </a>
        </div>
      </div>
    </footer>
  )
}
