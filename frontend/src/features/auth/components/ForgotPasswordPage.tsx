import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useForgotPassword } from '../hooks/usePasswordReset'
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '../schemas'
import { AuthPageShell, authLinkClassName } from './AuthPageShell'

export function ForgotPasswordPage() {
  const forgotPassword = useForgotPassword()
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })

  function onSubmit(values: ForgotPasswordFormValues) {
    forgotPassword.mutate(values.email)
  }

  return (
    <AuthPageShell>
      <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Esqueceu sua senha?</h2>

      {forgotPassword.isSuccess ? (
        // A API responde igual exista ou não a conta, então a mensagem também não pode afirmar que existe.
        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-300">
          Se houver uma conta cadastrada com <strong>{getValues('email')}</strong>, você vai receber um
          e-mail com o link para criar uma nova senha. Confira também a caixa de spam.
        </p>
      ) : (
        <>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Informe o e-mail da sua conta e enviaremos um link para você criar uma nova senha.
          </p>
          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
            <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
              <Input id="email" type="email" autoComplete="email" {...register('email')} />
            </FormField>
            {forgotPassword.isError && (
              <p className="text-sm text-red-600">
                {forgotPassword.error instanceof ApiError
                  ? forgotPassword.error.message
                  : 'Não foi possível enviar o link.'}
              </p>
            )}
            <Button type="submit" variant="brand" className="w-full" disabled={forgotPassword.isPending}>
              {forgotPassword.isPending ? 'Enviando...' : 'Enviar link'}
            </Button>
          </form>
        </>
      )}

      <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
        Lembrou a senha?{' '}
        <Link to="/login" className={authLinkClassName}>
          Voltar para o login
        </Link>
      </p>
    </AuthPageShell>
  )
}
