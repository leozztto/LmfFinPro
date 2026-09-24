import type { FocusEvent } from 'react'
import { Controller, useFormContext, useWatch } from 'react-hook-form'
import { FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { BRAZILIAN_STATES } from '@/shared/data/brazilianStates'
import { formatCep, formatCnpj, formatCpf, formatPhone, onlyDigits } from '@/shared/format/mask'
import { useCepLookup } from '../hooks/useCepLookup'
import { documentTypeForTaxRegime, TAX_REGIME_OPTIONS, type AccountFieldsValues } from '../schemas'

export const formSectionTitleClassName =
  'text-sm font-semibold uppercase tracking-wide text-zinc-400 dark:text-zinc-500'

/**
 * Seções de dados cadastrais (dados pessoais, documento e regime, endereço com busca de CEP),
 * compartilhadas entre o cadastro e o "Meu perfil". Precisa estar dentro de um <FormProvider>
 * cujo formulário contenha os campos de AccountFieldsValues.
 */
export function AccountDataFields() {
  const {
    register,
    control,
    setValue,
    formState: { errors },
  } = useFormContext<AccountFieldsValues>()

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

  return (
    <>
      <section className="space-y-4">
        <h2 className={formSectionTitleClassName}>Dados pessoais</h2>
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
        <h2 className={formSectionTitleClassName}>Documento e regime</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          <FormField label="Regime tributário" htmlFor="taxRegime" error={errors.taxRegime?.message}>
            <Select id="taxRegime" {...register('taxRegime')}>
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
        <h2 className={formSectionTitleClassName}>Endereço</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          <FormField label="CEP" htmlFor="address.zipCode" error={errors.address?.zipCode?.message ?? cepErrorMessage}>
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
            {cepLookup.isPending && <p className="mt-1 text-xs text-zinc-400">Buscando endereço...</p>}
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
            <Select id="address.state" disabled={cepLookup.isPending} {...register('address.state')}>
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
    </>
  )
}
