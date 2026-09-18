import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/shared/auth/AuthContext'
import { Button } from '@/shared/ui'
import { HomeIcon, SwapIcon, TagIcon, TransferIcon, WalletIcon } from '@/shared/ui/icons'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { Footer } from './Footer'

const NAV_ITEMS = [
  { to: '/', label: 'Visão geral', icon: HomeIcon },
  { to: '/contas', label: 'Contas', icon: WalletIcon },
  { to: '/categorias', label: 'Categorias', icon: TagIcon },
  { to: '/transacoes', label: 'Transações', icon: SwapIcon },
  { to: '/transferencias', label: 'Transferências', icon: TransferIcon },
]

export function AppLayout() {
  const { session, logout } = useAuth()

  return (
    <div className="flex min-h-screen flex-col bg-white dark:bg-zinc-900">
      <header className="px-6 py-5">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-y-2">
          <div className="flex items-baseline gap-2">
            <h1 className="text-xl font-semibold text-zinc-800 dark:text-zinc-100">FinPro</h1>
            <span className="hidden text-xs text-zinc-400 dark:text-zinc-500 sm:inline">
              Controle financeiro para freelancers e autônomos.
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-zinc-500 dark:text-zinc-400">{session?.name}</span>
            <ThemeToggle />
            <Button variant="secondary" onClick={logout}>
              Sair
            </Button>
          </div>
        </div>
      </header>

      <nav className="sticky top-0 z-40 border-b border-zinc-200 bg-white px-6 py-3 dark:border-zinc-800 dark:bg-zinc-900">
        <div className="mx-auto flex max-w-5xl flex-wrap justify-center gap-1 text-sm">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              title={item.label}
              className={({ isActive }) =>
                `flex items-center gap-0 border-b-2 px-3 py-2 font-medium transition-colors sm:gap-2 sm:px-4 ${
                  isActive
                    ? 'border-primary-600 text-primary-700 dark:border-primary-500 dark:text-primary-200'
                    : 'border-transparent text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50'
                }`
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              <span className="hidden sm:inline">{item.label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-10">
        <Outlet />
      </main>

      <Footer />
    </div>
  )
}
