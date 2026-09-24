import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useLogin } from '../hooks/useLogin'
import { loginSchema, type LoginFormValues } from '../schemas'
import { AuthPageShell, authLinkClassName } from './AuthPageShell'

/** Setado pela tela de redefinir senha ao concluir, para o login avisar que a senha mudou. */
export interface LoginLocationState {
  passwordReset?: boolean
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const passwordReset = (location.state as LoginLocationState | null)?.passwordReset === true
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })

  async function onSubmit(values: LoginFormValues) {
    await login.mutateAsync(values)
    navigate('/')
  }

  return (
    <AuthPageShell titlePrefix="Entrar no ">
      {passwordReset && (
        <p className="mb-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
          Senha redefinida com sucesso. Entre com a nova senha.
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" {...register('email')} />
        </FormField>
        <div>
          <FormField label="Senha" htmlFor="password" error={errors.password?.message}>
            <Input id="password" type="password" autoComplete="current-password" {...register('password')} />
          </FormField>
          <div className="mt-2 flex justify-end">
            <Link to="/esqueci-senha" className={`text-sm ${authLinkClassName}`}>
              Esqueceu sua senha?
            </Link>
          </div>
        </div>
        {login.isError && (
          <p className="text-sm text-red-600">
            {login.error instanceof ApiError ? login.error.message : 'Não foi possível entrar.'}
          </p>
        )}
        <Button type="submit" variant="brand" className="w-full" disabled={login.isPending}>
          {login.isPending ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>

      <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
        Ainda não tem conta?{' '}
        <Link to="/registro" className={authLinkClassName}>
          Cadastre-se
        </Link>
      </p>
    </AuthPageShell>
  )
}
