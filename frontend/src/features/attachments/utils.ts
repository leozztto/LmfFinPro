/** Mesmos limites do backend (TransactionAttachmentApplicationService) — checados antes do envio. */
export const MAX_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024
export const MAX_ATTACHMENTS_PER_TRANSACTION = 10
export const ACCEPTED_ATTACHMENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp']
export const ACCEPT_ATTRIBUTE = '.pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp'

/**
 * Validação rápida no navegador para dar retorno imediato. O backend confere de novo pelo conteúdo
 * do arquivo (não pela extensão), então isso é só conveniência.
 */
export function validateAttachmentFile(file: { size: number; type: string }, currentCount: number): string | null {
  if (currentCount >= MAX_ATTACHMENTS_PER_TRANSACTION) {
    return `Esta transação já tem ${MAX_ATTACHMENTS_PER_TRANSACTION} anexos, o máximo permitido.`
  }
  if (file.size === 0) return 'O arquivo está vazio.'
  if (file.size > MAX_ATTACHMENT_SIZE_BYTES) return 'O arquivo é grande demais. O limite é de 10 MB.'
  // Alguns navegadores deixam o tipo vazio: nesse caso quem decide é o backend.
  if (file.type && !ACCEPTED_ATTACHMENT_TYPES.includes(file.type)) {
    return 'Formato não aceito. Envie um PDF ou uma imagem JPG, PNG ou WEBP.'
  }
  return null
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`
}
