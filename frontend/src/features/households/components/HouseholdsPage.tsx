import { Card } from '@/shared/ui'
import { useHousehold } from '@/shared/household/HouseholdContext'
import { CreateHouseholdForm } from './CreateHouseholdForm'
import { ReceivedInvitesCard } from './ReceivedInvitesCard'
import { HouseholdCard } from './HouseholdCard'

/** Configurações → Grupos: grupos de casal/família, com contas, transações e relatórios em comum. */
export function HouseholdsPage() {
  const { sharedHouseholds, loading } = useHousehold()

  return (
    <div className="space-y-6">
      <ReceivedInvitesCard />

      <Card>
        <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">Grupos (casal ou família)</h3>
        <p className="mb-6 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          Seus dados pessoais ficam só com você. Num grupo, os membros veem e editam as mesmas contas, transações,
          orçamentos e relatórios. Cada pessoa mantém o próprio login e pode participar de mais de um grupo. Para
          alternar entre os seus dados e os de um grupo, use o seletor no topo da tela.
        </p>
        <CreateHouseholdForm />
      </Card>

      {loading && <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando grupos...</p>}

      {!loading && sharedHouseholds.length === 0 && (
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          Você ainda não participa de nenhum grupo. Crie um acima, aceite um convite recebido ou abra o link de um convite enviado por e-mail.
        </p>
      )}

      {sharedHouseholds.map((household) => (
        <HouseholdCard key={household.id} household={household} />
      ))}
    </div>
  )
}
