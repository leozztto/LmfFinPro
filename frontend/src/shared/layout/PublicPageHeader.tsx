import { useNavigate, useSearchParams } from 'react-router-dom'
import { ChevronLeftIcon } from '@/shared/ui/icons'
import { useAuth } from '@/shared/auth/AuthContext'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'

/** Parâmetro que marca quem chegou à página vindo da tela de cadastro (link aberto em outra aba). */
export const REGISTER_ORIGIN_PARAM = 'origem'
export const REGISTER_ORIGIN_VALUE = 'registro'

/**
 * Cabeçalho das páginas públicas fora do layout do app (suporte, termos, política): botão de voltar
 * (só o ícone, com "Voltar" na dica ao passar o mouse) à esquerda e alternância de tema à direita. O
 * voltar leva sempre ao mesmo lugar: o dashboard, se a pessoa estiver logada, ou o login, se não. A
 * exceção é quem veio do cadastro: o link abre em outra aba para não perder o formulário, então voltar
 * fecha esta aba (e a pessoa cai no cadastro que deixou aberto); se o navegador não deixar fechar, vai
 * para o cadastro.
 */
export function PublicPageHeader() {
  const navigate = useNavigate()
  const { session } = useAuth()
  const [searchParams] = useSearchParams()
  const fromRegister = !session && searchParams.get(REGISTER_ORIGIN_PARAM) === REGISTER_ORIGIN_VALUE

  function goBack() {
    if (!fromRegister) {
      navigate(session ? '/' : '/login')
      return
    }
    window.close()
    // Se a aba não fechou, o navegador recusou: leva ao cadastro em vez de ao login.
    navigate('/registro')
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl items-center justify-between gap-3 px-4 pt-4">
      <button
        type="button"
        onClick={goBack}
        aria-label="Voltar"
        title="Voltar"
        className="flex h-9 w-9 items-center justify-center rounded-lg border border-zinc-300 bg-transparent text-zinc-600 transition-colors hover:bg-zinc-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-800"
      >
        <ChevronLeftIcon />
      </button>
      <ThemeToggle />
    </div>
  )
}
