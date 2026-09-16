import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/features/auth/components/LoginPage'
import { RegisterPage } from '@/features/auth/components/RegisterPage'
import { DashboardPage } from '@/features/dashboard/components/DashboardPage'
import { AccountsPage } from '@/features/accounts/components/AccountsPage'
import { CategoriesPage } from '@/features/categories/components/CategoriesPage'
import { TransactionsPage } from '@/features/transactions/components/TransactionsPage'
import { AppLayout } from '@/shared/layout/AppLayout'
import { ProtectedRoute } from '@/shared/auth/ProtectedRoute'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/contas" element={<AccountsPage />} />
          <Route path="/categorias" element={<CategoriesPage />} />
          <Route path="/transacoes" element={<TransactionsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
