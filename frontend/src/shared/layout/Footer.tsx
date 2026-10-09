import { MailIcon, WhatsAppIcon } from '@/shared/ui/icons'
import { LEGAL_ENTITY } from '@/features/legal/legalEntity'
import { WHATSAPP_HREF } from '@/features/support/whatsapp'

/** Alvo de toque de 40px (acima do mínimo recomendado para celular) mesmo com ícone pequeno. */
const iconLinkClassName =
  'flex h-10 w-10 items-center justify-center rounded-lg text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-[#1ea883] active:text-[#1ea883] dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-[#2ad6a5] dark:active:text-[#2ad6a5]'

/** Rodapé do app: no celular o direito autoral vem em cima, os contatos embaixo e tudo fica
 *  centralizado; de `sm` em diante o direito autoral fica à esquerda e os contatos à direita. */
export function Footer() {
  const email = LEGAL_ENTITY.privacyEmail

  return (
    <footer className="border-t border-zinc-200 px-4 py-4 dark:border-zinc-800 sm:px-6">
      <div className="mx-auto flex max-w-5xl flex-col items-center gap-2 text-center sm:flex-row sm:justify-between sm:text-left">
        <p className="text-xs text-balance text-zinc-400 dark:text-zinc-500">
          © 2026 {LEGAL_ENTITY.name}. Todos os direitos reservados.
        </p>
        <nav aria-label="Contato" className="flex items-center justify-center gap-1">
          <a href={`mailto:${email}`} aria-label={`Enviar e-mail para ${email}`} title={email} className={iconLinkClassName}>
            <MailIcon />
          </a>
          <a
            href={WHATSAPP_HREF}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Conversar pelo WhatsApp"
            title="WhatsApp"
            className={iconLinkClassName}
          >
            <WhatsAppIcon />
          </a>
        </nav>
      </div>
    </footer>
  )
}
