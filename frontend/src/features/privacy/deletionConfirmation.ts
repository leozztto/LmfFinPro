/** Palavra que a pessoa digita para confirmar a exclusão, como segunda trava além da senha. */
export const DELETE_CONFIRMATION_WORD = 'EXCLUIR'

interface DeletionFormState {
  password: string
  confirmation: string
  /** Verdadeiro quando o servidor informou algum bloqueio na prévia. */
  blocked: boolean
  /** Verdadeiro enquanto a prévia não chegou. */
  loadingPreview: boolean
}

/** O botão "Excluir definitivamente" só habilita com senha, a palavra certa e sem bloqueios. */
export function canConfirmDeletion({ password, confirmation, blocked, loadingPreview }: DeletionFormState): boolean {
  return (
    !blocked &&
    !loadingPreview &&
    password.length > 0 &&
    confirmation.trim().toUpperCase() === DELETE_CONFIRMATION_WORD
  )
}
