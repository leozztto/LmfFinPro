import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, ColorInput, FormField, Input, Select } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCreateCategory } from '../hooks/useCreateCategory'
import { useUpdateCategory } from '../hooks/useUpdateCategory'
import { categorySchema, type CategoryFormValues } from '../schemas'
import { CATEGORY_TYPE_LABELS, type Category } from '../types'

interface CategoryFormProps {
  category?: Category
  onSuccess?: () => void
}

export function CategoryForm({ category, onSuccess }: CategoryFormProps) {
  const isEditing = category != null
  const createCategory = useCreateCategory()
  const updateCategory = useUpdateCategory()
  const isPending = isEditing ? updateCategory.isPending : createCategory.isPending
  const { showToast } = useToast()
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: isEditing
      ? { name: category.name, type: category.type, color: category.color ?? '' }
      : { type: 'EXPENSE', color: '' },
  })

  async function onSubmit(values: CategoryFormValues) {
    try {
      if (isEditing) {
        await updateCategory.mutateAsync({
          id: category.id,
          input: { name: values.name, type: values.type, color: values.color, icon: category.icon ?? undefined },
        })
        showToast('Categoria atualizada com sucesso.', 'success')
      } else {
        await createCategory.mutateAsync(values)
        reset({ name: '', type: 'EXPENSE', color: '', icon: '' })
        showToast('Categoria criada com sucesso.', 'success')
      }
      onSuccess?.()
    } catch (error) {
      showToast(
        error instanceof ApiError
          ? error.message
          : `Não foi possível ${isEditing ? 'atualizar' : 'criar'} a categoria.`,
      )
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2">
      <FormField label="Nome" htmlFor="category-name" error={errors.name?.message}>
        <Input id="category-name" placeholder="Consultoria" {...register('name')} />
      </FormField>
      <FormField label="Tipo" htmlFor="category-type" error={errors.type?.message}>
        <Select id="category-type" disabled={isEditing} {...register('type')}>
          {Object.entries(CATEGORY_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </FormField>
      <FormField label="Cor (opcional)" htmlFor="category-color" error={errors.color?.message}>
        <Controller
          name="color"
          control={control}
          render={({ field }) => (
            <ColorInput
              id="category-color"
              placeholder="#2E6E4E"
              value={field.value ?? ''}
              onChange={field.onChange}
              onBlur={field.onBlur}
            />
          )}
        />
      </FormField>
      <div className="sm:col-span-2">
        <Button type="submit" disabled={isPending} className="w-full">
          {isPending ? 'Salvando...' : isEditing ? 'Salvar alterações' : 'Adicionar categoria'}
        </Button>
      </div>
    </form>
  )
}
