import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/shared/auth/AuthContext'
import { Button } from '@/shared/ui'
import { Footer } from './Footer'

const NAV_ITEMS = [
  { to: '/', label: 'Visão geral' },
  { to: '/contas', label: 'Contas' },
  { to: '/categorias', label: 'Categorias' },
  { to: '/transacoes', label: 'Transações' },
]

export function AppLayout() {
  const { session, logout } = useAuth()

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 dark:bg-slate-900">
      <header className="border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <div className="flex items-baseline gap-2">
            <h1 className="text-xl font-bold text-slate-900 dark:text-slate-50">FinPro</h1>
            <span className="hidden text-xs text-slate-400 dark:text-slate-500 sm:inline">
              Controle financeiro para freelancers e autônomos.
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-slate-500 dark:text-slate-400">{session?.name}</span>
            <Button variant="secondary" onClick={logout}>
              Sair
            </Button>
          </div>
        </div>
        <nav className="mx-auto mt-4 flex max-w-5xl gap-4 text-sm">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                isActive
                  ? 'font-semibold text-primary-600'
                  : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-50'
              }
            >
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
