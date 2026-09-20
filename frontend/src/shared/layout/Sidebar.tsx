import { NavLink } from 'react-router-dom'
import { HomeIcon, PercentIcon, SwapIcon, TagIcon, TransferIcon, UploadIcon, UsersIcon, WalletIcon } from '@/shared/ui/icons'

const OVERVIEW_ITEM = { to: '/', label: 'Dashboard', icon: HomeIcon }

const NAV_SECTIONS = [
  {
    label: 'Cadastros',
    items: [
      { to: '/contas', label: 'Contas', icon: WalletIcon },
      { to: '/categorias', label: 'Categorias', icon: TagIcon },
    ],
  },
  {
    label: 'Financeiro',
    items: [
      { to: '/transacoes', label: 'Transações', icon: SwapIcon },
      { to: '/transferencias', label: 'Transferências', icon: TransferIcon },
      { to: '/importacoes', label: 'Importações', icon: UploadIcon },
    ],
  },
  {
    label: 'Freelancer',
    items: [
      { to: '/clientes', label: 'Clientes', icon: UsersIcon },
      { to: '/impostos', label: 'Impostos', icon: PercentIcon },
    ],
  },
]

const linkClassName = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive
      ? 'bg-primary-50 text-primary-700 dark:bg-zinc-800 dark:text-primary-200'
      : 'text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/60'
  }`

interface SidebarProps {
  onNavigate?: () => void
}

export function Sidebar({ onNavigate }: SidebarProps) {
  return (
    <nav className="flex h-full flex-col gap-6 overflow-y-auto p-4">
      <NavLink to={OVERVIEW_ITEM.to} end onClick={onNavigate} className={linkClassName}>
        <OVERVIEW_ITEM.icon className="h-4 w-4 shrink-0" />
        {OVERVIEW_ITEM.label}
      </NavLink>

      {NAV_SECTIONS.map((section) => (
        <div key={section.label}>
          <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
            {section.label}
          </p>
          <div className="flex flex-col gap-1">
            {section.items.map((item) => (
              <NavLink key={item.to} to={item.to} onClick={onNavigate} className={linkClassName}>
                <item.icon className="h-4 w-4 shrink-0" />
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
      ))}
    </nav>
  )
}
