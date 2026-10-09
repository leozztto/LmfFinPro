import type { ComponentType, SVGProps } from 'react'
import { NavLink } from 'react-router-dom'
import {
  BriefcaseIcon,
  CalendarIcon,
  FileTextIcon,
  HelpCircleIcon,
  HomeIcon,
  HeadsetIcon,
  PercentIcon,
  PiggyBankIcon,
  RepeatIcon,
  SwapIcon,
  TagIcon,
  TargetIcon,
  TransferIcon,
  TrendingUpIcon,
  UploadIcon,
  UsersIcon,
  WalletIcon,
} from '@/shared/ui/icons'

interface NavItem {
  to: string
  label: string
  icon: ComponentType<SVGProps<SVGSVGElement>>
  end?: boolean
}

/** Fora de qualquer seção: visões que cruzam Financeiro e Freelancer, não pertencem só a um. */
const TOP_LEVEL_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: HomeIcon, end: true },
  { to: '/relatorios', label: 'Relatórios', icon: FileTextIcon },
]

const NAV_SECTIONS: { label: string; items: NavItem[] }[] = [
  {
    label: 'Cadastros',
    items: [
      { to: '/contas', label: 'Contas', icon: WalletIcon },
      { to: '/categorias', label: 'Categorias e tags', icon: TagIcon },
    ],
  },
  {
    label: 'Financeiro',
    items: [
      { to: '/importacoes', label: 'Importações', icon: UploadIcon },
      { to: '/calendario', label: 'Calendário', icon: CalendarIcon },
      { to: '/transacoes', label: 'Transações', icon: SwapIcon },
      { to: '/transferencias', label: 'Transferências', icon: TransferIcon },
      { to: '/orcamentos', label: 'Orçamentos', icon: TargetIcon },
      { to: '/recorrencias', label: 'Recorrências', icon: RepeatIcon },
      { to: '/metas', label: 'Metas', icon: PiggyBankIcon },
      { to: '/patrimonio', label: 'Patrimônio', icon: TrendingUpIcon },
    ],
  },
  {
    label: 'Freelancer',
    items: [
      { to: '/clientes', label: 'Clientes', icon: UsersIcon },
      { to: '/impostos', label: 'Impostos', icon: PercentIcon },
      { to: '/pro-labore', label: 'Pró-labore', icon: BriefcaseIcon },
    ],
  },
]

const FOOTER_ITEMS: NavItem[] = [
  { to: '/faq', label: 'Documentação', icon: HelpCircleIcon },
  { to: '/suporte', label: 'Suporte', icon: HeadsetIcon },
]

const linkClassName =
  (collapsed: boolean) =>
  ({ isActive }: { isActive: boolean }) =>
    `flex items-center rounded-md py-2 text-sm font-medium transition-colors ${
      collapsed ? 'justify-center px-2' : 'gap-3 px-3'
    } ${
      isActive
        ? 'bg-[#2ad6a5]/10 text-[#1ea883] dark:bg-[#2ad6a5]/10 dark:text-[#2ad6a5]'
        : 'text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800/60'
    }`

interface SidebarProps {
  onNavigate?: () => void
  /** Só os ícones: o texto sai da tela, mas cada item mantém o nome acessível e a dica ao passar o mouse. */
  collapsed?: boolean
}

export function Sidebar({ onNavigate, collapsed = false }: SidebarProps) {
  const className = linkClassName(collapsed)

  function renderItem(item: NavItem) {
    return (
      <NavLink
        key={item.to}
        to={item.to}
        end={item.end}
        onClick={onNavigate}
        className={className}
        title={collapsed ? item.label : undefined}
      >
        <item.icon className="h-4 w-4 shrink-0" />
        <span className={collapsed ? 'sr-only' : undefined}>{item.label}</span>
      </NavLink>
    )
  }

  return (
    <nav
      aria-label="Menu principal"
      className={`scrollbar-none flex h-full flex-col gap-6 overflow-y-auto ${collapsed ? 'p-2' : 'p-4'}`}
    >
      <div className="flex flex-col gap-1">{TOP_LEVEL_ITEMS.map(renderItem)}</div>

      {NAV_SECTIONS.map((section) => (
        <div key={section.label}>
          {collapsed ? (
            <hr aria-hidden="true" className="mb-2 border-zinc-200 dark:border-zinc-800" />
          ) : (
            <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
              {section.label}
            </p>
          )}
          <div className="flex flex-col gap-1">{section.items.map(renderItem)}</div>
        </div>
      ))}

      <div className="mt-auto flex flex-col gap-1 border-t border-zinc-200 pt-4 dark:border-zinc-800">
        {FOOTER_ITEMS.map(renderItem)}
      </div>
    </nav>
  )
}
