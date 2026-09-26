import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/features/auth/components/LoginPage'
import { RegisterPage } from '@/features/auth/components/RegisterPage'
import { ForgotPasswordPage } from '@/features/auth/components/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/components/ResetPasswordPage'
import { DashboardPage } from '@/features/dashboard/components/DashboardPage'
import { AccountsPage } from '@/features/accounts/components/AccountsPage'
import { CategoriesPage } from '@/features/categories/components/CategoriesPage'
import { ClientsPage } from '@/features/clients/components/ClientsPage'
import { TransactionsPage } from '@/features/transactions/components/TransactionsPage'
import { RecurringTransactionsPage } from '@/features/recurringTransactions/components/RecurringTransactionsPage'
import { TransfersPage } from '@/features/transfers/components/TransfersPage'
import { ImportsPage } from '@/features/importBatches/components/ImportsPage'
import { TaxEstimatesPage } from '@/features/taxEstimates/components/TaxEstimatesPage'
import { BudgetsPage } from '@/features/budgets/components/BudgetsPage'
import { ReportsPage } from '@/features/reports/components/ReportsPage'
import { FaqPage } from "@/features/faq/components/FaqPage";
import { SettingsLayout } from '@/features/profile/components/SettingsLayout'
import { ProfileDataPage } from '@/features/profile/components/ProfileDataPage'
import { PasswordPage } from '@/features/profile/components/PasswordPage'
import { AppLayout } from '@/shared/layout/AppLayout'
import { ProtectedRoute } from '@/shared/auth/ProtectedRoute'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/contas" element={<AccountsPage />} />
          <Route path="/categorias" element={<CategoriesPage />} />
          <Route path="/clientes" element={<ClientsPage />} />
          <Route path="/transacoes" element={<TransactionsPage />} />
          <Route path="/recorrentes" element={<RecurringTransactionsPage />} />
          <Route path="/transferencias" element={<TransfersPage />} />
          <Route path="/importacoes" element={<ImportsPage />} />
          <Route path="/impostos" element={<TaxEstimatesPage />} />
          <Route path="/orcamentos" element={<BudgetsPage />} />
          <Route path="/relatorios" element={<ReportsPage />} />
          <Route path="/faq" element={<FaqPage />} />
          <Route path="/configuracoes" element={<SettingsLayout />}>
            <Route index element={<Navigate to="dados-cadastrais" replace />} />
            <Route path="dados-cadastrais" element={<ProfileDataPage />} />
            <Route path="senha" element={<PasswordPage />} />
          </Route>
          <Route path="/perfil" element={<Navigate to="/configuracoes/dados-cadastrais" replace />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
