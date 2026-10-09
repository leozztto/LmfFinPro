import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button, Modal } from '@/shared/ui'
import { DownloadIcon } from '@/shared/ui/icons'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { useCompleteOnboardingStep, useDismissOnboarding, useOnboarding } from '../hooks/useOnboarding'
import { ONBOARDING_PARAM, SAMPLE_CSV, TOUR_STEPS } from '../steps'
import { TourPreview } from './TourPreview'

const primaryButtonClassName =
  'inline-flex w-full items-center justify-center rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-700 disabled:opacity-60 sm:w-auto'

/**
 * Guia de primeiros passos, sobreposto a qualquer tela. Só mostra como fazer (onde clicar, o que preencher, onde
 * salvar), sem criar nada: a pessoa avança em "Próximo" e cada passo visto fica gravado no servidor, então ela retoma
 * do último ponto se não terminar. Abre sozinho enquanto o guia não foi concluído nem dispensado (ou com
 * {@code ?primeiros-passos=1}, usado no cadastro e nos e-mails) e fecha sem perder o lugar.
 */
export function OnboardingModal() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { data } = useOnboarding()
  const [autoOpen, setAutoOpen] = useState(false)
  const [closedHere, setClosedHere] = useState(false)
  const open = autoOpen || searchParams.has(ONBOARDING_PARAM)

  const pending = !!data && !data.dismissed && !data.completed
  useEffect(() => {
    if (pending && !closedHere) setAutoOpen(true)
  }, [pending, closedHere])

  const close = useCallback(() => {
    setAutoOpen(false)
    setClosedHere(true)
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current)
        next.delete(ONBOARDING_PARAM)
        return next
      },
      { replace: true },
    )
  }, [setSearchParams])

  return (
    <Modal open={open} onClose={close} title="Primeiros passos" size="lg">
      <Tour onClose={close} />
    </Modal>
  )
}

function Tour({ onClose }: { onClose: () => void }) {
  const { data, isLoading, isError } = useOnboarding()
  const completeStep = useCompleteOnboardingStep()
  const dismiss = useDismissOnboarding()
  const [picked, setPicked] = useState<number | null>(null)
  const [saveFailed, setSaveFailed] = useState(false)

  if (isLoading) return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando…</p>
  if (isError || !data) {
    return (
      <p role="alert" className="text-sm text-red-600">
        Não foi possível carregar seus primeiros passos agora. Tente novamente em instantes.
      </p>
    )
  }

  const total = TOUR_STEPS.length
  const doneIds = new Set(data.steps.filter((step) => step.done).map((step) => step.id))
  // Retoma do primeiro passo ainda não visto; quem já terminou e reabriu revê desde o começo.
  const firstPending = TOUR_STEPS.findIndex((step) => !doneIds.has(step.id))
  const index = picked ?? Math.max(firstPending, 0)
  const step = TOUR_STEPS[index]
  const isLast = index === total - 1

  async function next() {
    setSaveFailed(false)
    try {
      if (!doneIds.has(step.id)) await completeStep.mutateAsync(step.id)
    } catch {
      setSaveFailed(true)
      return
    }
    if (isLast) onClose()
    else setPicked(index + 1)
  }

  return (
    <div className="space-y-5">
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-3 text-xs text-zinc-500 dark:text-zinc-400">
          <span>{step.group}</span>
          <span>
            Passo {index + 1} de {total}
          </span>
        </div>
        <div
          role="progressbar"
          aria-label="Progresso dos primeiros passos"
          aria-valuemin={1}
          aria-valuemax={total}
          aria-valuenow={index + 1}
          className="h-1.5 w-full overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-700"
        >
          <div
            className="h-full rounded-full bg-[#1ea883] transition-all"
            style={{ width: `${((index + 1) / total) * 100}%` }}
          />
        </div>
      </div>

      <div className="space-y-1">
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">{step.title}</h3>
        <p className="text-sm text-zinc-600 dark:text-zinc-300">{step.description}</p>
        <p className="text-xs text-zinc-500 dark:text-zinc-400">Onde: {step.where}</p>
      </div>

      <TourPreview step={step.id} />

      {step.id === 'IMPORT_PICK' && (
        <div>
          <Button
            type="button"
            variant="secondary"
            onClick={() => downloadBlob(new Blob([SAMPLE_CSV], { type: 'text/csv' }), 'modelo-extrato.csv')}
            className="w-full sm:w-auto"
          >
            <DownloadIcon className="mr-2 h-4 w-4" />
            Baixar modelo de CSV
          </Button>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            Sem o extrato à mão? Baixe o modelo, edite com seus lançamentos e envie.
          </p>
        </div>
      )}

      {saveFailed && (
        <p role="alert" className="text-sm text-red-600">
          Não foi possível salvar o seu progresso agora. Tente novamente.
        </p>
      )}

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <Button
          type="button"
          variant="secondary"
          onClick={() => setPicked(index - 1)}
          disabled={index === 0}
          className="w-full sm:w-auto"
        >
          Voltar
        </Button>
        <button type="button" onClick={next} disabled={completeStep.isPending} className={primaryButtonClassName}>
          {isLast ? 'Concluir' : 'Próximo'}
        </button>
      </div>

      {!data.completed && (
        <p className="flex flex-col items-center gap-2 text-sm sm:flex-row sm:justify-center sm:gap-6">
          <button type="button" onClick={onClose} className="text-zinc-500 underline dark:text-zinc-400">
            Continuar depois
          </button>
          <button
            type="button"
            onClick={() => dismiss.mutate(undefined, { onSuccess: onClose })}
            disabled={dismiss.isPending}
            className="text-zinc-500 underline dark:text-zinc-400"
          >
            Não mostrar mais
          </button>
        </p>
      )}
    </div>
  )
}
