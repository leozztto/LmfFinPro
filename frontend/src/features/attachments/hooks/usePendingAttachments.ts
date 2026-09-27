import { useRef, useState } from 'react'
import type { AttachmentDocumentType, PendingAttachment } from '../types'
import { splitValidFiles } from '../utils'

/**
 * Fila de arquivos escolhidos e ainda não enviados. {@code existingCount} é quantos anexos a
 * transação já tem (zero na criação), para o limite por transação valer também na fila.
 */
export function usePendingAttachments(existingCount = 0) {
  const [items, setItems] = useState<PendingAttachment[]>([])
  const [rejections, setRejections] = useState<string[]>([])
  const nextKey = useRef(0)

  function add(files: File[]) {
    const { accepted, rejected } = splitValidFiles(files, existingCount + items.length)
    setRejections(rejected)
    setItems((current) => [
      ...current,
      ...accepted.map((file) => ({ key: nextKey.current++, file, documentType: 'PAYMENT_PROOF' as const })),
    ])
  }

  function changeType(key: number, documentType: AttachmentDocumentType) {
    setItems((current) => current.map((item) => (item.key === key ? { ...item, documentType } : item)))
  }

  function remove(key: number) {
    setItems((current) => current.filter((item) => item.key !== key))
  }

  /** Troca a fila pelo que sobrou de um envio (os que falharam, com o motivo). */
  function replace(next: PendingAttachment[]) {
    setItems(next)
    setRejections([])
  }

  return { items, rejections, add, changeType, remove, replace, clear: () => replace([]) }
}
