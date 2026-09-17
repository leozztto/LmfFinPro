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
      <header className="border-b border-zinc-200 bg-zinc-50 px-6 py-4 dark:border-zinc-700 dark:bg-zinc-800">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <div className="flex items-baseline gap-2">
            <h1 className="text-xl font-bold text-zinc-900 dark:text-zinc-50">FinPro</h1>
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
        <nav className="mx-auto mt-4 flex max-w-5xl justify-center gap-2 text-sm">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-4 py-2 font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-600 text-white'
                    : 'text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-700 dark:hover:text-zinc-50'
                }`
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-8">
        <Outlet />
      </main>

      <Footer />
    </div>
  )
}
