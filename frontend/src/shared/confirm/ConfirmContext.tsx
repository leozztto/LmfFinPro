import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { Button } from '@/shared/ui/Button'
import { Modal } from '@/shared/ui/Modal'

interface ConfirmOptions {
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
}

interface ConfirmContextValue {
  /** Mostra um modal de confirmação e resolve `true`/`false` conforme a escolha do usuário. */
  confirm: (options: ConfirmOptions) => Promise<boolean>
}

const ConfirmContext = createContext<ConfirmContextValue | undefined>(undefined)

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [options, setOptions] = useState<ConfirmOptions | null>(null)
  const resolveRef = useRef<((value: boolean) => void) | null>(null)

  const confirm = useCallback((nextOptions: ConfirmOptions) => {
    setOptions(nextOptions)
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve
    })
  }, [])

  function respond(value: boolean) {
    resolveRef.current?.(value)
    resolveRef.current = null
    setOptions(null)
  }

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      <Modal open={options != null} onClose={() => respond(false)} title={options?.title ?? 'Confirmar remoção'}>
        <p className="text-sm text-zinc-600 dark:text-zinc-300">{options?.message}</p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => respond(false)}>
            {options?.cancelLabel ?? 'Cancelar'}
          </Button>
          <Button variant="danger" onClick={() => respond(true)}>
            {options?.confirmLabel ?? 'Remover'}
          </Button>
        </div>
      </Modal>
    </ConfirmContext.Provider>
  )
}

export function useConfirm(): ConfirmContextValue['confirm'] {
  const context = useContext(ConfirmContext)
  if (!context) {
    throw new Error('useConfirm deve ser usado dentro de um ConfirmProvider')
  }
  return context.confirm
}
