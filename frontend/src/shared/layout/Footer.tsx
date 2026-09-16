import { GitHubIcon, GlobeIcon, LinkedInIcon, MailIcon } from '@/shared/ui/icons'

const EMAIL = 'leozztto@gmail.com'
const GITHUB_URL = 'https://github.com/leozztto'
const LINKEDIN_URL = 'https://www.linkedin.com/in/leandro-mf'
const PORTFOLIO_URL = 'https://portfolio-leandromf.vercel.app/'

const iconLinkClassName =
  'text-slate-500 transition-colors hover:text-primary-600 dark:text-slate-400 dark:hover:text-primary-500'

export function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-white px-6 py-6 dark:border-slate-700 dark:bg-slate-800">
      <div className="mx-auto flex max-w-5xl flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-xs text-slate-400 dark:text-slate-500">© 2026 Lezzotto Tech. Todos os direitos reservados.</p>
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
