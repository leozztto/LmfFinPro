import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useResetPassword } from '../hooks/usePasswordReset'
import { resetPasswordSchema, type ResetPasswordFormValues } from '../schemas'
import { AuthPageShell, authLinkClassName } from './AuthPageShell'
import type { LoginLocationState } from './LoginPage'

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const resetPassword = useResetPassword()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })

  async function onSubmit(values: ResetPasswordFormValues) {
    await resetPassword.mutateAsync({ token, password: values.password })
    const state: LoginLocationState = { passwordReset: true }
    navigate('/login', { replace: true, state })
  }

  return (
    <AuthPageShell>
      <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Criar nova senha</h2>

      {token ? (
        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
          <FormField label="Nova senha" htmlFor="password" error={errors.password?.message}>
            <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
          </FormField>
          <FormField label="Confirme a nova senha" htmlFor="confirmPassword" error={errors.confirmPassword?.message}>
            <Input id="confirmPassword" type="password" autoComplete="new-password" {...register('confirmPassword')} />
          </FormField>
          {resetPassword.isError && (
            <p className="text-sm text-red-600">
              {resetPassword.error instanceof ApiError
                ? resetPassword.error.message
                : 'Não foi possível redefinir a senha.'}
            </p>
          )}
          <Button type="submit" variant="brand" className="w-full" disabled={resetPassword.isPending}>
            {resetPassword.isPending ? 'Salvando...' : 'Salvar nova senha'}
          </Button>
        </form>
      ) : (
        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-300">
          Link de redefinição inválido. Abra o link exatamente como veio no e-mail ou solicite um novo.
        </p>
      )}

      <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
        <Link to="/esqueci-senha" className={authLinkClassName}>
          Solicitar um novo link
        </Link>
        {' · '}
        <Link to="/login" className={authLinkClassName}>
          Voltar para o login
        </Link>
      </p>
    </AuthPageShell>
  )
}
