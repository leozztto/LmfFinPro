import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useRegister } from '../hooks/useRegister'
import { registerSchema, type RegisterFormValues } from '../schemas'

export function RegisterPage() {
  const navigate = useNavigate()
  const registerUser = useRegister()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) })

  async function onSubmit(values: RegisterFormValues) {
    await registerUser.mutateAsync(values)
    navigate('/')
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 dark:bg-slate-900">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <h1 className="text-xl font-bold text-slate-900 dark:text-slate-50">Criar conta no FinPro</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Leva menos de um minuto — sem cartão, sem enrolação.
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
          <FormField label="Nome" htmlFor="name" error={errors.name?.message}>
            <Input id="name" type="text" autoComplete="name" {...register('name')} />
          </FormField>
          <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
            <Input id="email" type="email" autoComplete="email" {...register('email')} />
          </FormField>
          <FormField label="Senha" htmlFor="password" error={errors.password?.message}>
            <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
          </FormField>
          {registerUser.isError && (
            <p className="text-sm text-red-600">
              {registerUser.error instanceof ApiError
                ? registerUser.error.message
                : 'Não foi possível criar a conta.'}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={registerUser.isPending}>
            {registerUser.isPending ? 'Criando conta...' : 'Criar conta'}
          </Button>
        </form>

        <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-primary-600 hover:underline">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  )
}
