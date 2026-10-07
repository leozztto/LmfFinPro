import { Navigate, Outlet } from 'react-router-dom'
import { ConsentGate } from '@/features/legal/components/ConsentGate'
import { useAuth } from './AuthContext'

export function ProtectedRoute() {
  const { session, restoring } = useAuth()

  // Sem isso, recarregar a página mandaria o usuário para /login antes de o cookie de refresh
  // devolver o access token.
  if (restoring) {
    return (
      <div role="status" className="flex min-h-screen items-center justify-center text-sm text-zinc-500 dark:text-zinc-400">
        Carregando…
      </div>
    )
  }

  if (!session) {
    return <Navigate to="/login" replace />
  }

  // Documentos legais novos ou ainda não aceitos travam o app até o aceite (ver ConsentGate).
  return (
    <ConsentGate>
      <Outlet />
    </ConsentGate>
  )
}
