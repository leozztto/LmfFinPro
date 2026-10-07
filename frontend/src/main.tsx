import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '@/shared/auth/AuthContext'
import { HouseholdProvider } from '@/shared/household/HouseholdContext'
import { ThemeProvider } from '@/shared/theme/ThemeContext'
import { ToastProvider } from '@/shared/toast/ToastContext'
import { ConfirmProvider } from '@/shared/confirm/ConfirmContext'
import { AppRouter } from '@/app/router'
import './index.css'

const queryClient = new QueryClient()

// Service worker só para notificações push (ver public/sw.js). Falha em silêncio: o app funciona igual.
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => undefined)
  })
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <ToastProvider>
            <AuthProvider>
              <HouseholdProvider>
                <ConfirmProvider>
                  <AppRouter />
                </ConfirmProvider>
              </HouseholdProvider>
            </AuthProvider>
          </ToastProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
