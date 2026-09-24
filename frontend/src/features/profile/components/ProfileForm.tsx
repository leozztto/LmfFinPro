import { useMemo } from 'react'
import { FormProvider, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatCep, formatCnpj, formatCpf, formatPhone, onlyDigits } from '@/shared/format/mask'
import { AccountDataFields } from '@/features/auth/components/AccountDataFields'
import {
  documentTypeForTaxRegime,
  emailChanged,
  makeProfileSchema,
  type ProfileFormValues,
} from '@/features/auth/schemas'
import { useUpdateProfile } from '../hooks/useProfile'
import type { Profile } from '../types'

function toFormValues(profile: Profile): ProfileFormValues {
  const formatDocument = profile.documentType === 'CNPJ' ? formatCnpj : formatCpf
  return {
    name: profile.name,
    email: profile.email,
    taxRegime: profile.taxRegime,
    documentNumber: formatDocument(profile.documentNumber),
    phone: profile.phone ? formatPhone(profile.phone) : '',
    address: {
      zipCode: formatCep(profile.address?.zipCode ?? ''),
      street: profile.address?.street ?? '',
      number: profile.address?.number ?? '',
      complement: profile.address?.complement ?? '',
      neighborhood: profile.address?.neighborhood ?? '',
      city: profile.address?.city ?? '',
      state: profile.address?.state ?? '',
    },
    currentPassword: '',
  }
}

export function ProfileForm({ profile }: { profile: Profile }) {
  const { showToast } = useToast()
  const updateProfile = useUpdateProfile()
  const resolver = useMemo(() => zodResolver(makeProfileSchema(profile.email)), [profile.email])
  const form = useForm<ProfileFormValues>({
    resolver,
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: toFormValues(profile),
  })
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = form

  const email = useWatch({ control, name: 'email' })
  const needsCurrentPassword = emailChanged(profile.email, email ?? '')

  async function onSubmit(values: ProfileFormValues) {
    const saved = await updateProfile.mutateAsync({
      name: values.name,
      email: values.email,
      documentType: documentTypeForTaxRegime(values.taxRegime),
      documentNumber: values.documentNumber,
      phone: values.phone ? onlyDigits(values.phone) : undefined,
      taxRegime: values.taxRegime,
      address: {
        ...values.address,
        zipCode: onlyDigits(values.address.zipCode),
        complement: values.address.complement || undefined,
      },
      currentPassword: needsCurrentPassword ? values.currentPassword : undefined,
    })
    reset(toFormValues(saved))
    showToast('Dados cadastrais atualizados.', 'success')
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        <AccountDataFields />

        {needsCurrentPassword && (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-900 dark:bg-amber-950/40">
            <p className="mb-3 text-sm text-amber-800 dark:text-amber-300">
              O e-mail é o seu login. Para alterá-lo, confirme sua senha atual.
            </p>
            <div className="sm:max-w-xs">
              <FormField label="Senha atual" htmlFor="profileCurrentPassword" error={errors.currentPassword?.message}>
                <Input
                  id="profileCurrentPassword"
                  type="password"
                  autoComplete="current-password"
                  {...register('currentPassword')}
                />
              </FormField>
            </div>
          </div>
        )}

        {updateProfile.isError && (
          <p className="text-sm text-red-600">
            {updateProfile.error instanceof ApiError
              ? updateProfile.error.message
              : 'Não foi possível salvar os dados.'}
          </p>
        )}

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="secondary"
            disabled={!isDirty || updateProfile.isPending}
            onClick={() => {
              reset(toFormValues(profile))
              updateProfile.reset()
            }}
          >
            Descartar alterações
          </Button>
          <Button type="submit" variant="brand" disabled={!isDirty || updateProfile.isPending}>
            {updateProfile.isPending ? 'Salvando...' : 'Salvar alterações'}
          </Button>
        </div>
      </form>
    </FormProvider>
  )
}
