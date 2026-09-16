import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useLogin } from '../hooks/useLogin'
import { loginSchema, type LoginFormValues } from '../schemas'
import { Footer } from '@/shared/layout/Footer'

export function LoginPage() {
  const navigate = useNavigate()
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  async function onSubmit(values: LoginFormValues) {
    await login.mutateAsync(values)
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 dark:bg-slate-900">
      <div className="flex flex-1 items-center justify-center px-4">
        <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
          <h1 className="text-xl font-bold text-slate-900 dark:text-slate-50">Entrar no FinPro</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Controle financeiro para freelancers e autônomos.
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
            <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" autoComplete="email" {...register('email')} />
            </FormField>
            <FormField label="Senha" htmlFor="password" error={errors.password?.message}>
              <Input id="password" type="password" autoComplete="current-password" {...register('password')} />
            </FormField>
            {login.isError && (
              <p className="text-sm text-red-600">
                {login.error instanceof ApiError ? login.error.message : 'Não foi possível entrar.'}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={login.isPending}>
              {login.isPending ? 'Entrando...' : 'Entrar'}
            </Button>
          </form>

          <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
            Ainda não tem conta?{' '}
            <Link to="/registro" className="font-medium text-primary-600 hover:underline">
              Cadastre-se
            </Link>
          </p>
        </div>
      </div>

      <Footer />
    </div>
  )
}
