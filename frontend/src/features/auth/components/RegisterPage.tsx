import { FormProvider, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Checkbox, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { onlyDigits } from '@/shared/format/mask'
import { useLegalVersions } from '@/features/legal/hooks/useLegal'
import { useRegister } from '../hooks/useRegister'
import { documentTypeForTaxRegime, registerSchema, type RegisterFormValues } from '../schemas'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { authLinkClassName } from './AuthPageShell'
import { AccountDataFields, formSectionTitleClassName } from './AccountDataFields'

export function RegisterPage() {
  const navigate = useNavigate()
  const registerUser = useRegister()
  const inviteToken = useSearchParams()[0].get('convite') || undefined
  const { data: legalVersions, isError: legalVersionsFailed } = useLegalVersions()
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: {
      name: '',
      email: '',
      taxRegime: '',
      documentNumber: '',
      phone: '',
      address: {
        zipCode: '',
        street: '',
        number: '',
        complement: '',
        neighborhood: '',
        city: '',
        state: '',
      },
      password: '',
      confirmPassword: '',
      acceptedTerms: false,
    },
  })
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form

  async function onSubmit(values: RegisterFormValues) {
    // Sem as versões vigentes não há o que aceitar: o botão fica desabilitado até elas chegarem.
    if (!legalVersions) return
    await registerUser.mutateAsync({
      name: values.name,
      email: values.email,
      password: values.password,
      documentType: documentTypeForTaxRegime(values.taxRegime),
      documentNumber: values.documentNumber,
      phone: values.phone ? onlyDigits(values.phone) : undefined,
      taxRegime: values.taxRegime,
      address: {
        ...values.address,
        zipCode: onlyDigits(values.address.zipCode),
        complement: values.address.complement || undefined,
      },
      inviteToken,
      termsVersion: legalVersions.termsVersion,
      privacyVersion: legalVersions.privacyVersion,
    })
    navigate('/')
  }

  return (
    <div className="flex min-h-dvh flex-col bg-white dark:bg-zinc-900">
      <div className="flex justify-end px-4 pt-4">
        <ThemeToggle />
      </div>
      <div className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-lg rounded-xl border border-zinc-200 bg-zinc-50 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-800 sm:p-8 lg:max-w-3xl">
          <h1 className="text-xl font-semibold text-zinc-800 dark:text-zinc-100">Criar conta no FinPro</h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Leva menos de um minuto — sem cartão, sem enrolação.
          </p>

          {inviteToken && (
            <p className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
              Você foi convidado para um grupo. Ao criar a conta, você já entra nele; seus dados pessoais continuam só
              seus.
            </p>
          )}

          <FormProvider {...form}>
            <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-8">
              <AccountDataFields />

              <section className="space-y-4">
                <h2 className={formSectionTitleClassName}>Segurança</h2>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <FormField label="Senha" htmlFor="password" error={errors.password?.message}>
                    <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
                  </FormField>
                  <FormField label="Confirmar senha" htmlFor="confirmPassword" error={errors.confirmPassword?.message}>
                    <Input
                      id="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      {...register('confirmPassword')}
                    />
                  </FormField>
                </div>
              </section>

              <div>
                <label htmlFor="acceptedTerms" className="flex items-start gap-2 text-sm text-zinc-600 dark:text-zinc-300">
                  <Checkbox id="acceptedTerms" className="mt-0.5" {...register('acceptedTerms')} />
                  <span>
                    Li e aceito os{' '}
                    <Link to="/termos" target="_blank" rel="noopener noreferrer" className={authLinkClassName}>
                      Termos de Uso
                    </Link>{' '}
                    e a{' '}
                    <Link to="/privacidade" target="_blank" rel="noopener noreferrer" className={authLinkClassName}>
                      Política de Privacidade
                    </Link>{' '}
                    do FinPro.
                  </span>
                </label>
                {errors.acceptedTerms && <p className="mt-1 text-sm text-red-600">{errors.acceptedTerms.message}</p>}
              </div>

              {registerUser.isError && (
                <p className="text-sm text-red-600">
                  {registerUser.error instanceof ApiError
                    ? registerUser.error.message
                    : 'Não foi possível criar a conta.'}
                </p>
              )}
              {legalVersionsFailed && (
                <p role="alert" className="text-sm text-red-600">
                  Não foi possível carregar os Termos de Uso. Recarregue a página para tentar de novo.
                </p>
              )}

              <Button type="submit" className="w-full" disabled={registerUser.isPending || !legalVersions}>
                {registerUser.isPending ? 'Criando conta...' : 'Criar conta'}
              </Button>
            </form>
          </FormProvider>

          <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
            Já tem conta?{' '}
            <Link to="/login" className={authLinkClassName}>
              Entrar
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
