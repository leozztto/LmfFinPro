import { useRef, type ChangeEvent } from 'react'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { Button } from '@/shared/ui'
import { useProfilePhotoUrl, useRemoveProfilePhoto, useUploadProfilePhoto } from '../hooks/useProfilePhoto'
import { UserAvatar } from './UserAvatar'

const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_SIZE_BYTES = 2 * 1024 * 1024

interface ProfilePhotoEditorProps {
  name: string
  hasPhoto: boolean
}

/** Foto de perfil (opcional): adicionar/trocar e remover. A validação de verdade (formato pelo
 *  conteúdo, tamanho) é do backend; aqui só se evita subir um arquivo óbvio demais. */
export function ProfilePhotoEditor({ name, hasPhoto }: ProfilePhotoEditorProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const photoUrl = useProfilePhotoUrl()
  const upload = useUploadProfilePhoto()
  const remove = useRemoveProfilePhoto()
  const { showToast } = useToast()
  const isBusy = upload.isPending || remove.isPending

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    if (!ACCEPTED_TYPES.includes(file.type)) {
      showToast('Formato não aceito. Envie uma imagem JPG, PNG ou WEBP.')
      return
    }
    if (file.size > MAX_SIZE_BYTES) {
      showToast('A foto é grande demais. O limite é de 2 MB.')
      return
    }
    upload.mutate(file, {
      onSuccess: () => showToast('Foto atualizada.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível enviar a foto.'),
    })
  }

  function handleRemove() {
    remove.mutate(undefined, {
      onSuccess: () => showToast('Foto removida.', 'success'),
      onError: (error) => showToast(error instanceof ApiError ? error.message : 'Não foi possível remover a foto.'),
    })
  }

  return (
    <div className="mb-6 flex flex-col items-center gap-4 sm:flex-row">
      <UserAvatar photoUrl={photoUrl} name={name} className="h-24 w-24" iconClassName="h-10 w-10" />
      <div className="flex flex-col items-center gap-2 sm:items-start">
        <div className="flex flex-wrap justify-center gap-2 sm:justify-start">
          <Button type="button" variant="secondary" onClick={() => inputRef.current?.click()} disabled={isBusy}>
            {upload.isPending ? 'Enviando...' : hasPhoto ? 'Trocar foto' : 'Adicionar foto'}
          </Button>
          {hasPhoto && (
            <Button type="button" variant="secondary" onClick={handleRemove} disabled={isBusy}>
              {remove.isPending ? 'Removendo...' : 'Remover'}
            </Button>
          )}
        </div>
        <p className="text-center text-xs text-zinc-500 dark:text-zinc-400 sm:text-left">
          Opcional. JPG, PNG ou WEBP, até 2 MB.
        </p>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(',')}
          className="hidden"
          onChange={handleFileChange}
          aria-label="Escolher foto de perfil"
        />
      </div>
    </div>
  )
}
