import { NavLink, Outlet } from 'react-router-dom'
import { BellIcon, LockIcon, ShieldIcon, UserIcon, UsersIcon } from '@/shared/ui/icons'
import { PendingInvitesBadge } from '@/features/households/components/PendingInvitesBadge'

const SETTINGS_ITEMS = [
  { to: 'dados-cadastrais', label: 'Dados cadastrais', icon: UserIcon },
  { to: 'senha', label: 'Alterar senha', icon: LockIcon },
  { to: 'notificacoes', label: 'Notificações', icon: BellIcon },
  { to: 'grupos', label: 'Grupos', icon: UsersIcon },
  { to: 'privacidade', label: 'Privacidade', icon: ShieldIcon },
]

/**
 * No desktop o submenu fica numa coluna à esquerda; no celular vira abas no topo que dividem a
 * largura em partes iguais (ícone sobre o texto), sem rolagem horizontal.
 */
const linkClassName = ({ isActive }: { isActive: boolean }) =>
  `-mb-px flex min-w-0 flex-col items-center justify-center gap-1 border-b-2 px-1 py-2 text-center text-xs font-medium leading-tight transition-colors md:mb-0 md:flex-row md:justify-start md:gap-2 md:whitespace-nowrap md:rounded-md md:border-b-0 md:px-3 md:text-left md:text-sm ${
    isActive
      ? 'border-[#2ad6a5] text-[#1ea883] md:bg-[#2ad6a5]/10 dark:text-[#2ad6a5]'
      : 'border-transparent text-zinc-500 hover:text-zinc-800 md:hover:bg-zinc-100 dark:text-zinc-400 dark:hover:text-zinc-100 md:dark:hover:bg-zinc-800/60'
  }`

export function SettingsLayout() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Configurações</h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Seus dados cadastrais, a senha de acesso, os alertas por e-mail, os grupos de casal ou família e a privacidade dos seus dados.
        </p>
      </div>

      <div className="flex flex-col gap-6 md:flex-row md:gap-8">
        <nav
          aria-label="Configurações"
          className="grid grid-cols-3 border-b sm:grid-cols-5 border-zinc-200 dark:border-zinc-700 md:flex md:w-48 md:shrink-0 md:flex-col md:gap-1 md:self-start md:border-b-0"
        >
          {SETTINGS_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} className={linkClassName}>
              <item.icon className="h-4 w-4 shrink-0" />
              {item.label}
              {item.to === 'grupos' && <PendingInvitesBadge />}
            </NavLink>
          ))}
        </nav>

        <div className="min-w-0 flex-1">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
