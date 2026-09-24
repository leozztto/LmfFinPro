import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, FormField, Input } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { changePasswordSchema, type ChangePasswordFormValues } from '@/features/auth/schemas'
import { useChangePassword } from '../hooks/useProfile'

const EMPTY_VALUES: ChangePasswordFormValues = { currentPassword: '', newPassword: '', confirmPassword: '' }

export function ChangePasswordForm() {
  const { showToast } = useToast()
  const changePassword = useChangePassword()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: EMPTY_VALUES,
  })

  async function onSubmit(values: ChangePasswordFormValues) {
    await changePassword.mutateAsync({ currentPassword: values.currentPassword, newPassword: values.newPassword })
    reset(EMPTY_VALUES)
    showToast('Senha alterada. As outras sessões abertas foram encerradas.', 'success')
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Senha atual" htmlFor="currentPassword" error={errors.currentPassword?.message}>
          <Input id="currentPassword" type="password" autoComplete="current-password" {...register('currentPassword')} />
        </FormField>
        <FormField label="Nova senha" htmlFor="newPassword" error={errors.newPassword?.message}>
          <Input id="newPassword" type="password" autoComplete="new-password" {...register('newPassword')} />
        </FormField>
        <FormField label="Confirmar nova senha" htmlFor="confirmNewPassword" error={errors.confirmPassword?.message}>
          <Input id="confirmNewPassword" type="password" autoComplete="new-password" {...register('confirmPassword')} />
        </FormField>
      </div>

      {changePassword.isError && (
        <p className="text-sm text-red-600">
          {changePassword.error instanceof ApiError
            ? changePassword.error.message
            : 'Não foi possível alterar a senha.'}
        </p>
      )}

      <div className="flex justify-end">
        <Button type="submit" variant="brand" className="w-full sm:w-auto" disabled={changePassword.isPending}>
          {changePassword.isPending ? 'Alterando...' : 'Alterar senha'}
        </Button>
      </div>
    </form>
  )
}
