import type { FocusEvent } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Checkbox, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { BRAZILIAN_STATES } from '@/shared/data/brazilianStates'
import { formatCep, formatCnpj, formatCpf, formatPhone, onlyDigits } from '@/shared/format/mask'
import { useRegister } from '../hooks/useRegister'
import { useCepLookup } from '../hooks/useCepLookup'
import { documentTypeForTaxRegime, registerSchema, TAX_REGIME_OPTIONS, type RegisterFormValues } from '../schemas'
import { Footer } from '@/shared/layout/Footer'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'

export function RegisterPage() {
  const navigate = useNavigate()
  const registerUser = useRegister()
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<RegisterFormValues>({
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

  const taxRegime = useWatch({ control, name: 'taxRegime' })
  const documentType = documentTypeForTaxRegime(taxRegime || 'AUTONOMO')
  const documentLabel = documentType === 'CNPJ' ? 'CNPJ' : 'CPF'
  const formatDocument = documentType === 'CNPJ' ? formatCnpj : formatCpf

  const cepLookup = useCepLookup()
  const cepErrorMessage = cepLookup.isError
    ? cepLookup.error instanceof ApiError && cepLookup.error.status === 404
      ? 'CEP não encontrado. Preencha o endereço manualmente.'
      : 'Não foi possível buscar o CEP agora. Preencha o endereço manualmente.'
    : undefined

  function handleZipCodeBlur(event: FocusEvent<HTMLInputElement>) {
    const digits = onlyDigits(event.target.value)
    if (digits.length !== 8) return
    cepLookup.mutate(digits, {
      onSuccess: (address) => {
        setValue('address.street', address.street, { shouldValidate: true, shouldDirty: true })
        setValue('address.neighborhood', address.neighborhood, { shouldValidate: true, shouldDirty: true })
        setValue('address.city', address.city, { shouldValidate: true, shouldDirty: true })
        setValue('address.state', address.state, { shouldValidate: true, shouldDirty: true })
      },
    })
  }

  async function onSubmit(values: RegisterFormValues) {
    await registerUser.mutateAsync({
      name: values.name,
      email: values.email,
      password: values.password,
      documentType: documentTypeForTaxRegime(values.taxRegime),
      documentNumber: values.documentNumber,
      phone: values.phone || undefined,
      taxRegime: values.taxRegime,
      address: {
        ...values.address,
        zipCode: onlyDigits(values.address.zipCode),
        complement: values.address.complement || undefined,
      },
    })
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col bg-white dark:bg-zinc-900">
      <div className="flex justify-end px-4 pt-4">
        <ThemeToggle />
      </div>
      <div className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-lg rounded-xl border border-zinc-200 bg-zinc-50 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-800 sm:p-8 lg:max-w-3xl">
          <h1 className="text-xl font-bold text-zinc-900 dark:text-zinc-50">Criar conta no FinPro</h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Leva menos de um minuto — sem cartão, sem enrolação.
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-8">
            <section className="space-y-4">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
                Dados pessoais
              </h2>
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField label="Nome completo" htmlFor="name" error={errors.name?.message}>
                  <Input id="name" type="text" autoComplete="name" placeholder="Ana Freelancer" {...register('name')} />
                </FormField>
                <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
                  <Input id="email" type="email" autoComplete="email" {...register('email')} />
                </FormField>
              </div>
            </section>

            <section className="space-y-4">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
                Documento e regime
              </h2>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField label="Regime tributário" htmlFor="taxRegime" error={errors.taxRegime?.message}>
                  <Select id="taxRegime" defaultValue="" {...register('taxRegime')}>
                    <option value="" disabled>
                      Selecione...
                    </option>
                    {TAX_REGIME_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </Select>
                </FormField>
                <FormField label={documentLabel} htmlFor="documentNumber" error={errors.documentNumber?.message}>
                  <Controller
                    name="documentNumber"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="documentNumber"
                        type="text"
                        inputMode="numeric"
                        placeholder={documentType === 'CNPJ' ? '00.000.000/0000-00' : '000.000.000-00'}
                        value={field.value ?? ''}
                        onChange={(event) => field.onChange(formatDocument(event.target.value))}
                        onBlur={field.onBlur}
                      />
                    )}
                  />
                </FormField>
                <FormField label="Telefone (opcional)" htmlFor="phone" error={errors.phone?.message}>
                  <Controller
                    name="phone"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="phone"
                        type="tel"
                        inputMode="numeric"
                        placeholder="(11) 98765-4321"
                        value={field.value ?? ''}
                        onChange={(event) => field.onChange(formatPhone(event.target.value))}
                        onBlur={field.onBlur}
                      />
                    )}
                  />
                </FormField>
              </div>
            </section>

            <section className="space-y-4">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
                Endereço
              </h2>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField
                  label="CEP"
                  htmlFor="address.zipCode"
                  error={errors.address?.zipCode?.message ?? cepErrorMessage}
                >
                  <Controller
                    name="address.zipCode"
                    control={control}
                    render={({ field }) => (
                      <Input
                        id="address.zipCode"
                        type="text"
                        inputMode="numeric"
                        placeholder="00000-000"
                        value={field.value ?? ''}
                        onChange={(event) => {
                          field.onChange(formatCep(event.target.value))
                          if (cepLookup.isError) cepLookup.reset()
                        }}
                        onBlur={(event) => {
                          field.onBlur()
                          handleZipCodeBlur(event)
                        }}
                      />
                    )}
                  />
                  {cepLookup.isPending && (
                    <p className="mt-1 text-xs text-zinc-400">Buscando endereço...</p>
                  )}
                </FormField>
                <FormField label="Número" htmlFor="address.number" error={errors.address?.number?.message}>
                  <Input id="address.number" type="text" {...register('address.number')} />
                </FormField>
                <FormField label="Complemento (opcional)" htmlFor="address.complement" error={errors.address?.complement?.message}>
                  <Input id="address.complement" type="text" placeholder="Apto, sala..." {...register('address.complement')} />
                </FormField>
              </div>
              <FormField label="Logradouro" htmlFor="address.street" error={errors.address?.street?.message}>
                <Input
                  id="address.street"
                  type="text"
                  placeholder="Rua, avenida..."
                  disabled={cepLookup.isPending}
                  {...register('address.street')}
                />
              </FormField>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField label="Bairro" htmlFor="address.neighborhood" error={errors.address?.neighborhood?.message}>
                  <Input
                    id="address.neighborhood"
                    type="text"
                    disabled={cepLookup.isPending}
                    {...register('address.neighborhood')}
                  />
                </FormField>
                <FormField label="Cidade" htmlFor="address.city" error={errors.address?.city?.message}>
                  <Input id="address.city" type="text" disabled={cepLookup.isPending} {...register('address.city')} />
                </FormField>
                <FormField label="Estado" htmlFor="address.state" error={errors.address?.state?.message}>
                  <Select id="address.state" defaultValue="" disabled={cepLookup.isPending} {...register('address.state')}>
                    <option value="" disabled>
                      UF
                    </option>
                    {BRAZILIAN_STATES.map((state) => (
                      <option key={state.value} value={state.value}>
                        {state.value}
                      </option>
                    ))}
                  </Select>
                </FormField>
              </div>
            </section>

            <section className="space-y-4">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500">
                Segurança
              </h2>
              <div className="grid gap-4 sm:grid-cols-2">
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
                Li e aceito os termos de uso e a política de privacidade do FinPro.
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

            <Button type="submit" className="w-full" disabled={registerUser.isPending}>
              {registerUser.isPending ? 'Criando conta...' : 'Criar conta'}
            </Button>
          </form>

          <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
            Já tem conta?{' '}
            <Link to="/login" className="font-medium text-primary-600 hover:underline">
              Entrar
            </Link>
          </p>
        </div>
      </div>

      <Footer />
    </div>
  )
}
