import { useCallback, useState } from 'react'

export const SIDEBAR_COLLAPSED_KEY = 'finpro.sidebar.collapsed'

function readStored(): boolean {
  try {
    return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1'
  } catch {
    return false
  }
}

/** Menu lateral recolhido (só ícones) ou aberto; a escolha fica neste navegador. */
export function useSidebarCollapsed() {
  const [collapsed, setCollapsed] = useState(readStored)

  const toggle = useCallback(() => {
    setCollapsed((current) => {
      const next = !current
      try {
        localStorage.setItem(SIDEBAR_COLLAPSED_KEY, next ? '1' : '0')
      } catch {
        // Sem armazenamento (janela privada, por exemplo): vale só até recarregar.
      }
      return next
    })
  }, [])

  return { collapsed, toggle }
}
