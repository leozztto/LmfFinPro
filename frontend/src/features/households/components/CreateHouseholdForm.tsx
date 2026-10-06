import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { useCreateHousehold } from '../hooks/useHouseholds'
import { householdSchema, type HouseholdFormValues } from '../schemas'

export function CreateHouseholdForm() {
  const createHousehold = useCreateHousehold()
  const { showToast } = useToast()
  const { switchTo } = useHousehold()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<HouseholdFormValues>({
    resolver: zodResolver(householdSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { name: '' },
  })

  async function onSubmit(values: HouseholdFormValues) {
    try {
      const created = await createHousehold.mutateAsync(values.name)
      reset()
      showToast(`Grupo "${created.name}" criado. Convide as pessoas abaixo.`, 'success')
      switchTo(created.id)
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar o grupo.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3 sm:flex-row sm:items-start" noValidate>
      <div className="min-w-0 flex-1">
        <FormField label="Nome do grupo" htmlFor="household-name" error={errors.name?.message}>
          <Input id="household-name" placeholder="Ex.: Família Silva" autoComplete="off" {...register('name')} />
        </FormField>
      </div>
      <Button type="submit" className="w-full sm:mt-6 sm:w-auto" disabled={createHousehold.isPending}>
        {createHousehold.isPending ? 'Criando...' : 'Criar grupo'}
      </Button>
    </form>
  )
}
