import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { CloseIcon, MenuIcon } from '@/shared/ui/icons'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import logoIcon from '@/shared/assets/finpro-logo-icon.png'
import { Footer } from './Footer'
import { Sidebar } from './Sidebar'
import { UserMenu } from './UserMenu'

export function AppLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  useEffect(() => {
    if (!mobileNavOpen) return

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') setMobileNavOpen(false)
    }

    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', handleEscape)
    }
  }, [mobileNavOpen])

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-white dark:bg-zinc-900">
      <header className="z-30 flex shrink-0 items-center justify-between gap-3 border-b border-zinc-200 bg-white px-4 py-4 dark:border-zinc-800 dark:bg-zinc-900 sm:px-6">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setMobileNavOpen(true)}
            aria-label="Abrir menu"
            className="-ml-1 rounded-md p-2 text-zinc-500 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800 md:hidden"
          >
            <MenuIcon className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-2">
            <img src={logoIcon} alt="FinPro" className="h-8 w-8" />
            <div className="flex items-baseline gap-2">
              <h1 className="text-xl font-semibold">
                <span className="text-zinc-800 dark:text-zinc-100">Fin</span>
                <span className="text-[#2ad6a5]">Pro</span>
              </h1>
              <span className="hidden text-xs text-zinc-400 dark:text-zinc-500 lg:inline">
                Seu financeiro no controle
              </span>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <ThemeToggle />
          <UserMenu />
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="hidden w-56 shrink-0 overflow-y-auto border-r border-zinc-200 dark:border-zinc-800 md:block">
          <Sidebar />
        </aside>

        {mobileNavOpen && (
          <div className="fixed inset-0 z-50 md:hidden">
            <div
              className="absolute inset-0 bg-black/40"
              onClick={() => setMobileNavOpen(false)}
              aria-hidden="true"
            />
            <div className="relative flex h-full w-64 max-w-[80vw] flex-col bg-white shadow-xl dark:bg-zinc-900">
              <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-4 dark:border-zinc-800">
                <div className="flex items-center gap-2">
                  <img src={logoIcon} alt="FinPro" className="h-7 w-7" />
                  <span className="text-lg font-semibold">
                    <span className="text-zinc-800 dark:text-zinc-100">Fin</span>
                    <span className="text-[#2ad6a5]">Pro</span>
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setMobileNavOpen(false)}
                  aria-label="Fechar menu"
                  className="rounded-md p-1.5 text-zinc-500 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
                >
                  <CloseIcon className="h-5 w-5" />
                </button>
              </div>
              <Sidebar onNavigate={() => setMobileNavOpen(false)} />
            </div>
          </div>
        )}

        <main className="flex-1 overflow-y-auto">
          <div className="flex min-h-full flex-col">
            <div className="mx-auto w-full max-w-5xl flex-1 px-6 py-10">
              <Outlet />
            </div>
            <Footer />
          </div>
        </main>
      </div>
    </div>
  )
}
