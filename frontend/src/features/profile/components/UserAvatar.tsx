import { UserIcon } from '@/shared/ui/icons'

interface UserAvatarProps {
  /** URL da foto; sem ela (é opcional) mostra o ícone de usuário. */
  photoUrl: string | null
  name?: string
  /** Classes de tamanho (ex.: "h-9 w-9"). */
  className?: string
  iconClassName?: string
}

export function UserAvatar({ photoUrl, name, className = 'h-9 w-9', iconClassName = 'h-5 w-5' }: UserAvatarProps) {
  return (
    <span
      className={`flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-zinc-200 bg-zinc-50 text-zinc-600 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 ${className}`}
    >
      {photoUrl ? (
        <img src={photoUrl} alt={name ? `Foto de ${name}` : 'Foto de perfil'} className="h-full w-full object-cover" />
      ) : (
        <UserIcon className={iconClassName} />
      )}
    </span>
  )
}
