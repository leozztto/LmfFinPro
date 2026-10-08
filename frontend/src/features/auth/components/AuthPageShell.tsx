import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { LifeBuoyIcon } from '@/shared/ui/icons'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import logoIcon from '@/shared/assets/finpro-logo-icon.png'

interface AuthPageShellProps {
  /** Texto antes de "FinPro" no título (ex: "Entrar no "). */
  titlePrefix?: string
  children: ReactNode
}

/** Layout das telas públicas de acesso (login, esqueci a senha, redefinir senha): logo + título
 *  acima de um box central. */
export function AuthPageShell({ titlePrefix = '', children }: AuthPageShellProps) {
  return (
    <div className="flex min-h-dvh flex-col bg-white dark:bg-zinc-900">
      <div className="flex justify-end gap-2 px-4 pt-4">
        <Link
          to="/suporte"
          aria-label="Suporte e contato"
          title="Suporte e contato"
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-zinc-300 bg-transparent text-zinc-600 transition-colors hover:bg-zinc-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-800"
        >
          <LifeBuoyIcon />
        </Link>
        <ThemeToggle />
      </div>
      <div className="flex flex-1 items-center justify-center px-4 py-6">
        <div className="w-full max-w-sm">
          {/* Tamanhos em cqw (relativos à largura do bloco) para logo + textos ficarem proporcionais
              à largura do box em qualquer tela. */}
          <div className="@container mb-6">
            <div className="flex items-center justify-center gap-[3cqw]">
              <img src={logoIcon} alt="FinPro" className="size-[14cqw]" />
              <div>
                <h1 className="text-[8.2cqw] leading-tight font-semibold">
                  <span className="text-zinc-800 dark:text-zinc-100">{titlePrefix}Fin</span>
                  <span className="text-[#2ad6a5]">Pro</span>
                </h1>
                <p className="mt-[0.5cqw] text-[3.6cqw] leading-tight text-zinc-500 dark:text-zinc-400">
                  Seu financeiro no controle
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-800 sm:p-8">
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}

export const authLinkClassName =
  'font-medium text-blue-600 hover:text-blue-700 hover:underline dark:text-blue-400 dark:hover:text-blue-300'
