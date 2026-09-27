import { useForm, type FieldErrors, type UseFormRegisterReturn } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { getCurrentIsoDate } from '@/shared/format/date'
import { useCreateDebt, useUpdateDebt } from '../hooks/useNetWorth'
import {
  debtCreateSchema,
  debtUpdateSchema,
  type DebtCreateFormValues,
  type DebtUpdateFormValues,
} from '../schemas'
import { DEBT_TYPE_LABELS, type NetWorthDebtRow } from '../types'

interface DebtFormProps {
  /** Sem dívida: criação (com o saldo devedor atual). Com dívida: edição de nome, tipo e credor. */
  debt?: NetWorthDebtRow
  onSuccess?: () => void
}

export function DebtForm({ debt, onSuccess }: DebtFormProps) {
  return debt ? <EditDebtForm debt={debt} onSuccess={onSuccess} /> : <CreateDebtForm onSuccess={onSuccess} />
}

function CreateDebtForm({ onSuccess }: { onSuccess?: () => void }) {
  const createDebt = useCreateDebt()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<DebtCreateFormValues>({
    resolver: zodResolver(debtCreateSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { type: 'FINANCING', balanceDate: getCurrentIsoDate() },
  })

  async function onSubmit(values: DebtCreateFormValues) {
    try {
      await createDebt.mutateAsync({ ...values, creditor: values.creditor || null })
      showToast('Dívida cadastrada.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível cadastrar a dívida.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <DebtDetailsFields
        fields={{ name: register('name'), type: register('type'), creditor: register('creditor') }}
        errors={errors}
      />
      <FormField
        label="Saldo devedor atual (R$)"
        htmlFor="debt-balance"
        error={errors.balance?.message}
        hint="Quanto falta pagar, pelo extrato do banco."
      >
        <Input id="debt-balance" type="number" step="0.01" inputMode="decimal" {...register('balance')} />
      </FormField>
      <FormField label="Data do saldo" htmlFor="debt-balance-date" error={errors.balanceDate?.message}>
        <Input id="debt-balance-date" type="date" max={getCurrentIsoDate()} {...register('balanceDate')} />
      </FormField>
      <p className="text-xs text-zinc-500 dark:text-zinc-400 sm:col-span-2">
        As parcelas continuam sendo lançadas como despesas. Atualize o saldo devedor de tempos em tempos para o
        patrimônio acompanhar.
      </p>
      <Button type="submit" disabled={createDebt.isPending} className="w-full sm:col-span-2">
        {createDebt.isPending ? 'Salvando...' : 'Cadastrar dívida'}
      </Button>
    </form>
  )
}

function EditDebtForm({ debt, onSuccess }: { debt: NetWorthDebtRow; onSuccess?: () => void }) {
  const updateDebt = useUpdateDebt()
  const { showToast } = useToast()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<DebtUpdateFormValues>({
    resolver: zodResolver(debtUpdateSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { name: debt.name, type: debt.type, creditor: debt.creditor ?? '' },
  })

  async function onSubmit(values: DebtUpdateFormValues) {
    try {
      await updateDebt.mutateAsync({ id: debt.debtId, input: { ...values, creditor: values.creditor || null } })
      showToast('Dívida atualizada.', 'success')
      onSuccess?.()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar a dívida.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <DebtDetailsFields
        fields={{ name: register('name'), type: register('type'), creditor: register('creditor') }}
        errors={errors}
      />
      <Button type="submit" disabled={updateDebt.isPending} className="w-full sm:col-span-2">
        {updateDebt.isPending ? 'Salvando...' : 'Salvar'}
      </Button>
    </form>
  )
}

/**
 * Campos nome, tipo e credor, comuns à criação e à edição. Recebe o resultado de `register` de
 * cada campo, para servir aos dois formulários (que têm tipos de valores diferentes).
 */
interface DebtDetailsFieldsProps {
  fields: Record<'name' | 'type' | 'creditor', UseFormRegisterReturn>
  errors: FieldErrors<DebtUpdateFormValues>
}

function DebtDetailsFields({ fields, errors }: DebtDetailsFieldsProps) {
  return (
    <>
      <FormField label="Nome" htmlFor="debt-name" error={errors.name?.message}>
        <Input id="debt-name" placeholder="Financiamento do carro" {...fields.name} />
      </FormField>
      <FormField label="Tipo" htmlFor="debt-type" error={errors.type?.message}>
        <Select id="debt-type" {...fields.type}>
          {Object.entries(DEBT_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Credor (opcional)" htmlFor="debt-creditor" error={errors.creditor?.message}>
        <Input id="debt-creditor" placeholder="Banco" {...fields.creditor} />
      </FormField>
    </>
  )
}
