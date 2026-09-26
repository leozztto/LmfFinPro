import { NavLink, Outlet } from 'react-router-dom'
import { BellIcon, LockIcon, UserIcon } from '@/shared/ui/icons'

const SETTINGS_ITEMS = [
  { to: 'dados-cadastrais', label: 'Dados cadastrais', icon: UserIcon },
  { to: 'senha', label: 'Alterar senha', icon: LockIcon },
  { to: 'notificacoes', label: 'Notificações', icon: BellIcon },
]

/** No desktop o submenu fica numa coluna à esquerda; no celular vira abas horizontais no topo. */
const linkClassName = ({ isActive }: { isActive: boolean }) =>
  `flex shrink-0 items-center gap-2 whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium transition-colors md:rounded-md md:border-b-0 ${
    isActive
      ? 'border-[#2ad6a5] text-[#1ea883] md:bg-[#2ad6a5]/10 dark:text-[#2ad6a5]'
      : 'border-transparent text-zinc-500 hover:text-zinc-800 md:hover:bg-zinc-100 dark:text-zinc-400 dark:hover:text-zinc-100 md:dark:hover:bg-zinc-800/60'
  }`

export function SettingsLayout() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Configurações</h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Seus dados cadastrais, a senha de acesso e os alertas por e-mail.</p>
      </div>

      <div className="flex flex-col gap-6 md:flex-row md:gap-8">
        <nav
          aria-label="Configurações"
          className="flex gap-1 overflow-x-auto border-b border-zinc-200 dark:border-zinc-700 md:w-48 md:shrink-0 md:flex-col md:self-start md:border-b-0"
        >
          {SETTINGS_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} className={linkClassName}>
              <item.icon className="h-4 w-4 shrink-0" />
              {item.label}
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
