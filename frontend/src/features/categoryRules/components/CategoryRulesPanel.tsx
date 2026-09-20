import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Card, FormField, IconButton, Input, Select } from '@/shared/ui'
import { TrashIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useCategoryRules } from '../hooks/useCategoryRules'
import { useCreateCategoryRule } from '../hooks/useCreateCategoryRule'
import { useDeleteCategoryRule } from '../hooks/useDeleteCategoryRule'
import { categoryRuleSchema, type CategoryRuleFormValues } from '../schemas'

export function CategoryRulesPanel() {
  const { data: rules, isLoading } = useCategoryRules()
  const { data: categories } = useCategories()
  const createRule = useCreateCategoryRule()
  const deleteRule = useDeleteCategoryRule()
  const { showToast } = useToast()
  const confirm = useConfirm()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryRuleFormValues>({ resolver: zodResolver(categoryRuleSchema), mode: 'onBlur' })

  const categoryNameById = new Map(categories?.map((category) => [category.id, category.name]))
  const categoryColorById = new Map(categories?.map((category) => [category.id, category.color]))

  async function onSubmit(values: CategoryRuleFormValues) {
    try {
      await createRule.mutateAsync(values)
      reset()
      showToast('Regra criada com sucesso.', 'success')
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível criar a regra.')
    }
  }

  async function handleDelete(ruleId: number, pattern: string) {
    const confirmed = await confirm({
      message: `Remover a regra "${pattern}"? Transações futuras com esse descritivo deixarão de ser categorizadas automaticamente.`,
    })
    if (!confirmed) return

    deleteRule.mutate(ruleId, {
      onSuccess: () => showToast('Regra removida.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a regra.'),
    })
  }

  return (
    <Card>
      <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">Regras de categorização</h3>
      <p className="text-xs text-zinc-500 dark:text-zinc-400">
        Quando o descritivo de uma transação importada contém o padrão, ela é categorizada automaticamente. A cada
        correção manual na revisão de uma importação, uma regra é criada ou reforçada.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-4 grid gap-3 sm:grid-cols-[1fr_1fr_auto]">
        <FormField label="Padrão no descritivo" htmlFor="rule-pattern" error={errors.pattern?.message}>
          <Input id="rule-pattern" placeholder="Ex: UBER" {...register('pattern')} />
        </FormField>
        <FormField label="Categoria" htmlFor="rule-category" error={errors.categoryId?.message}>
          <Select id="rule-category" defaultValue="" {...register('categoryId')}>
            <option value="" disabled>
              Selecione...
            </option>
            {categories?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
        </FormField>
        <div className="flex items-end">
          <Button type="submit" disabled={createRule.isPending} className="w-full sm:w-auto">
            Adicionar
          </Button>
        </div>
      </form>

      <div className="mt-4 space-y-2">
        {isLoading ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando regras...</p>
        ) : !rules?.length ? (
          <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma regra cadastrada ainda.</p>
        ) : (
          rules.map((rule) => (
            <div
              key={rule.id}
              className="flex items-center justify-between gap-3 rounded-lg border border-zinc-200 px-3 py-2 text-sm dark:border-zinc-700"
            >
              <div className="flex min-w-0 items-center gap-2">
                <span
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: categoryColorById.get(rule.categoryId) ?? '#94a3b8' }}
                  aria-hidden
                />
                <span className="truncate font-medium text-zinc-800 dark:text-zinc-100">{rule.pattern}</span>
                <span className="shrink-0 text-zinc-500 dark:text-zinc-400">
                  → {categoryNameById.get(rule.categoryId) ?? 'categoria removida'}
                </span>
                <span className="shrink-0 rounded-full bg-zinc-100 px-2 py-0.5 text-xs text-zinc-500 dark:bg-zinc-700 dark:text-zinc-400">
                  peso {rule.weight}
                </span>
              </div>
              <IconButton
                icon={TrashIcon}
                label="Remover regra"
                onClick={() => handleDelete(rule.id, rule.pattern)}
                disabled={deleteRule.isPending}
                className="shrink-0"
              />
            </div>
          ))
        )}
      </div>
    </Card>
  )
}
