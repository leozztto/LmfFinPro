import { Card, Checkbox } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useOnboarding, useSetActivationEmails } from '../hooks/useOnboarding'

/** Liga e desliga os e-mails de boas-vindas e de primeiros passos (só nos primeiros dias de uso). */
export function ActivationEmailsCard() {
  const { data } = useOnboarding()
  const setActivationEmails = useSetActivationEmails()
  const { showToast } = useToast()

  if (!data) return null

  async function handleChange(enabled: boolean) {
    try {
      await setActivationEmails.mutateAsync(enabled)
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : 'Não foi possível salvar a preferência.')
    }
  }

  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">E-mails de primeiros passos</h3>
      <p className="mb-4 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Nos primeiros dias, enviamos as boas-vindas e até dois lembretes para quem ainda não importou nenhum lançamento.
        Quando você lança o primeiro movimento, os lembretes param.
      </p>
      <label className="flex items-start gap-3 text-sm text-zinc-700 dark:text-zinc-300">
        <Checkbox
          checked={data.activationEmailsEnabled}
          disabled={setActivationEmails.isPending}
          onChange={(event) => void handleChange(event.target.checked)}
          className="mt-0.5"
        />
        Quero receber os e-mails de primeiros passos
      </label>
    </Card>
  )
}
