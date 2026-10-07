import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/features/auth/components/LoginPage'
import { RegisterPage } from '@/features/auth/components/RegisterPage'
import { ForgotPasswordPage } from '@/features/auth/components/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/components/ResetPasswordPage'
import { InvitePage } from '@/features/households/components/InvitePage'
import { HouseholdsPage } from '@/features/households/components/HouseholdsPage'
import { DashboardPage } from '@/features/dashboard/components/DashboardPage'
import { AccountsPage } from '@/features/accounts/components/AccountsPage'
import { CategoriesPage } from '@/features/categories/components/CategoriesPage'
import { ClientsPage } from '@/features/clients/components/ClientsPage'
import { TransactionsPage } from '@/features/transactions/components/TransactionsPage'
import { RecurringTransactionsPage } from '@/features/recurringTransactions/components/RecurringTransactionsPage'
import { TransfersPage } from '@/features/transfers/components/TransfersPage'
import { CalendarPage } from '@/features/calendar/components/CalendarPage'
import { NetWorthPage } from '@/features/net-worth/components/NetWorthPage'
import { ImportsPage } from '@/features/importBatches/components/ImportsPage'
import { TaxEstimatesPage } from '@/features/taxEstimates/components/TaxEstimatesPage'
import { BudgetsPage } from '@/features/budgets/components/BudgetsPage'
import { ReportsPage } from '@/features/reports/components/ReportsPage'
import { FaqPage } from "@/features/faq/components/FaqPage";
import { SavingsGoalsPage } from '@/features/savings-goals/components/SavingsGoalsPage'
import { ProLaborePage } from '@/features/pro-labore/components/ProLaborePage'
import { SettingsLayout } from '@/features/profile/components/SettingsLayout'
import { ProfileDataPage } from '@/features/profile/components/ProfileDataPage'
import { PasswordPage } from '@/features/profile/components/PasswordPage'
import { NotificationsPage } from '@/features/profile/components/NotificationsPage'
import { AppLayout } from '@/shared/layout/AppLayout'
import { ProtectedRoute } from '@/shared/auth/ProtectedRoute'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
      <Route path="/convite" element={<InvitePage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/contas" element={<AccountsPage />} />
          <Route path="/categorias" element={<CategoriesPage />} />
          <Route path="/clientes" element={<ClientsPage />} />
          <Route path="/transacoes" element={<TransactionsPage />} />
          <Route path="/recorrencias" element={<RecurringTransactionsPage />} />
          <Route path="/calendario" element={<CalendarPage />} />
          <Route path="/transferencias" element={<TransfersPage />} />
          <Route path="/importacoes" element={<ImportsPage />} />
          <Route path="/impostos" element={<TaxEstimatesPage />} />
          <Route path="/orcamentos" element={<BudgetsPage />} />
          <Route path="/metas" element={<SavingsGoalsPage />} />
          <Route path="/patrimonio" element={<NetWorthPage />} />
          <Route path="/pro-labore" element={<ProLaborePage />} />
          <Route path="/relatorios" element={<ReportsPage />} />
          <Route path="/faq" element={<FaqPage />} />
          <Route path="/configuracoes" element={<SettingsLayout />}>
            <Route index element={<Navigate to="dados-cadastrais" replace />} />
            <Route path="dados-cadastrais" element={<ProfileDataPage />} />
            <Route path="senha" element={<PasswordPage />} />
            <Route path="notificacoes" element={<NotificationsPage />} />
            <Route path="grupos" element={<HouseholdsPage />} />
          </Route>
          <Route path="/perfil" element={<Navigate to="/configuracoes/dados-cadastrais" replace />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
