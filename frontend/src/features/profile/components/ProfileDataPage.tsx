import { Card } from '@/shared/ui'
import { useProfile } from '../hooks/useProfile'
import { ProfileForm } from './ProfileForm'

export function ProfileDataPage() {
  const profile = useProfile()

  return (
    <Card>
      <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Dados cadastrais</h3>
      <p className="mb-6 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        Os mesmos dados informados no cadastro. Para trocar o e-mail, que é o seu login, será pedida a senha atual.
      </p>

      {profile.isPending && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando...</p>}
      {profile.isError && <p className="text-sm text-red-600">Não foi possível carregar seus dados.</p>}
      {profile.data && <ProfileForm profile={profile.data} />}
    </Card>
  )
}
