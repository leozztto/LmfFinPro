import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Checkbox, ColorInput, FormField, Input, Select, Textarea } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { formatCnpj, formatCpf, formatPhone, onlyDigits } from '@/shared/format/mask'
import { useCreateClient } from '../hooks/useCreateClient'
import { useUpdateClient } from '../hooks/useUpdateClient'
import { clientSchema, type ClientFormValues } from '../schemas'
import { CLIENT_WORK_TYPE_LABELS, type Client } from '../types'

interface ClientFormProps {
  client?: Client
  onSuccess?: () => void
}

const EMPTY_VALUES: ClientFormValues = {
  name: '',
  email: '',
  phone: '',
  documentType: '',
  documentNumber: '',
  workType: '',
  notes: '',
  color: '',
  active: true,
}

function toFormValues(client?: Client): ClientFormValues {
  if (!client) return EMPTY_VALUES
  return {
    name: client.name,
    email: client.email ?? '',
    phone: client.phone ? formatPhone(client.phone) : '',
    documentType: client.documentType ?? '',
    documentNumber:
      client.documentNumber && client.documentType
        ? client.documentType === 'CNPJ'
          ? formatCnpj(client.documentNumber)
          : formatCpf(client.documentNumber)
        : '',
    workType: client.workType ?? '',
    notes: client.notes ?? '',
    color: client.color ?? '',
    active: client.active,
  }
}

export function ClientForm({ client, onSuccess }: ClientFormProps) {
  const isEditing = client != null
  const createClient = useCreateClient()
  const updateClient = useUpdateClient()
  const isPending = isEditing ? updateClient.isPending : createClient.isPending
  const { showToast } = useToast()
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ClientFormValues>({
    resolver: zodResolver(clientSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: toFormValues(client),
  })

  const documentType = useWatch({ control, name: 'documentType' })
  const formatDocument = documentType === 'CNPJ' ? formatCnpj : formatCpf

  async function onSubmit(values: ClientFormValues) {
    const input = {
      name: values.name,
      email: values.email || undefined,
      phone: values.phone ? onlyDigits(values.phone) : undefined,
      documentType: values.documentType || undefined,
      documentNumber: values.documentNumber ? onlyDigits(values.documentNumber) : undefined,
      workType: values.workType || undefined,
      notes: values.notes || undefined,
      color: values.color || undefined,
      active: values.active,
    }

    try {
      if (isEditing) {
        await updateClient.mutateAsync({ id: client.id, input })
        showToast('Cliente atualizado com sucesso.', 'success')
      } else {
        await createClient.mutateAsync(input)
        reset(EMPTY_VALUES)
        showToast('Cliente criado com sucesso.', 'success')
      }
      onSuccess?.()
    } catch (error) {
      showToast(
        error instanceof ApiError
          ? error.message
          : `Não foi possível ${isEditing ? 'atualizar' : 'criar'} o cliente.`,
      )
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Nome" htmlFor="client-name" error={errors.name?.message}>
        <Input id="client-name" placeholder="Empresa X" {...register('name')} />
      </FormField>
      <FormField label="E-mail" htmlFor="client-email" error={errors.email?.message}>
        <Input id="client-email" type="email" placeholder="contato@empresa.com" {...register('email')} />
      </FormField>
      <FormField label="Telefone" htmlFor="client-phone" error={errors.phone?.message}>
        <Controller
          name="phone"
          control={control}
          render={({ field }) => (
            <Input
              id="client-phone"
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
      <FormField label="Tipo de trabalho" htmlFor="client-work-type" error={errors.workType?.message}>
        <Select id="client-work-type" defaultValue="" {...register('workType')}>
          <option value="" disabled>
            Selecione...
          </option>
          {Object.entries(CLIENT_WORK_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Documento" htmlFor="client-document-type" error={errors.documentType?.message}>
        <Select id="client-document-type" defaultValue="" {...register('documentType')}>
          <option value="" disabled>
            Selecione...
          </option>
          <option value="CPF">CPF</option>
          <option value="CNPJ">CNPJ</option>
        </Select>
      </FormField>
      <FormField label="Número do documento" htmlFor="client-document-number" error={errors.documentNumber?.message}>
        <Controller
          name="documentNumber"
          control={control}
          render={({ field }) => (
            <Input
              id="client-document-number"
              type="text"
              inputMode="numeric"
              disabled={!documentType}
              placeholder={documentType === 'CNPJ' ? '00.000.000/0000-00' : '000.000.000-00'}
              value={field.value ?? ''}
              onChange={(event) => field.onChange(formatDocument(event.target.value))}
              onBlur={field.onBlur}
            />
          )}
        />
      </FormField>
      <FormField label="Cor (opcional)" htmlFor="client-color" error={errors.color?.message}>
        <Controller
          name="color"
          control={control}
          render={({ field }) => (
            <ColorInput
              id="client-color"
              placeholder="#2E6E4E"
              value={field.value ?? ''}
              onChange={field.onChange}
              onBlur={field.onBlur}
            />
          )}
        />
      </FormField>
      <div className="sm:col-span-2">
        <FormField label="Observações (opcional)" htmlFor="client-notes" error={errors.notes?.message}>
          <Textarea id="client-notes" rows={3} placeholder="Anotações sobre o cliente/projeto" {...register('notes')} />
        </FormField>
      </div>
      <div className="sm:col-span-2">
        <label htmlFor="client-active" className="flex items-center gap-2 text-sm text-zinc-600 dark:text-zinc-300">
          <Checkbox id="client-active" {...register('active')} />
          Cliente ativo
        </label>
      </div>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={isPending} className="w-full">
          {isPending ? 'Salvando...' : isEditing ? 'Salvar alterações' : 'Adicionar cliente'}
        </Button>
      </div>
    </form>
  )
}
