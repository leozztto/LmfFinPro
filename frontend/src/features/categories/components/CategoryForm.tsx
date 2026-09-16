import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input, Select } from '@/shared/ui'
import { useCreateCategory } from '../hooks/useCreateCategory'
import { categorySchema, type CategoryFormValues } from '../schemas'
import { CATEGORY_TYPE_LABELS } from '../types'

export function CategoryForm() {
  const createCategory = useCreateCategory()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { type: 'EXPENSE' },
  })

  async function onSubmit(values: CategoryFormValues) {
    await createCategory.mutateAsync(values)
    reset({ name: '', type: 'EXPENSE', color: '', icon: '' })
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-3">
      <FormField label="Nome" htmlFor="category-name" error={errors.name?.message}>
        <Input id="category-name" placeholder="Consultoria" {...register('name')} />
      </FormField>
      <FormField label="Tipo" htmlFor="category-type" error={errors.type?.message}>
        <Select id="category-type" {...register('type')}>
          {Object.entries(CATEGORY_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Cor (opcional)" htmlFor="category-color" error={errors.color?.message}>
        <Input id="category-color" placeholder="#2E6E4E" {...register('color')} />
      </FormField>
      <div className="sm:col-span-3">
        <Button type="submit" disabled={createCategory.isPending}>
          {createCategory.isPending ? 'Salvando...' : 'Adicionar categoria'}
        </Button>
      </div>
    </form>
  )
}
