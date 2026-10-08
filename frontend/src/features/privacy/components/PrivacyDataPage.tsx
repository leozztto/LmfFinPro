import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card } from '@/shared/ui'
import { ApiError } from '@/shared/api/httpClient'
import { formatDateOnlyBr } from '@/shared/format/date'
import { authLinkClassName } from '@/features/auth/components/AuthPageShell'
import { useConsentStatus } from '@/features/legal/hooks/useLegal'
import type { DocumentConsent } from '@/features/legal/types'
import { useExportData } from '../hooks/usePrivacy'
import { DeleteAccountModal } from './DeleteAccountModal'

const sectionTitleClassName = 'text-base font-semibold text-zinc-800 dark:text-zinc-100'
const sectionTextClassName = 'mt-1 text-sm text-zinc-500 dark:text-zinc-400'

/** Direitos do titular (LGPD): documentos aceitos, cópia dos dados e exclusão da conta. */
export function PrivacyDataPage() {
  return (
    <div className="space-y-6">
      <ConsentCard />
      <ExportCard />
      <DeleteAccountCard />
    </div>
  )
}

function ConsentCard() {
  const status = useConsentStatus()
  const data = status.data

  return (
    <Card>
      <h3 className={sectionTitleClassName}>Termos e política de privacidade</h3>
      <p className={sectionTextClassName}>
        Guardamos a versão de cada documento que você aceitou e a data do aceite.
      </p>

      {data && (
        <dl className="mt-4 space-y-3 text-sm">
          <ConsentRow label="Termos de Uso" to="/termos" consent={data.terms} />
          <ConsentRow label="Política de Privacidade" to="/privacidade" consent={data.privacy} />
        </dl>
      )}

      <p className="mt-4 text-xs text-zinc-500 dark:text-zinc-400">
        Os Termos e a Política são condição para usar o FinPro. Para retirar o consentimento, exclua a conta mais
        abaixo. As notificações por e-mail e push você desliga em Notificações.
      </p>
    </Card>
  )
}

function ConsentRow({ label, to, consent }: { label: string; to: string; consent: DocumentConsent }) {
  return (
    <div className="flex flex-col gap-0.5 sm:flex-row sm:items-baseline sm:justify-between sm:gap-4">
      <dt>
        <Link to={to} target="_blank" rel="noopener noreferrer" className={authLinkClassName}>
          {label}
        </Link>
      </dt>
      <dd className="text-zinc-600 dark:text-zinc-300">
        {consent.accepted && consent.acceptedAt
          ? `Versão ${consent.acceptedVersion} aceita em ${formatDateOnlyBr(consent.acceptedAt.slice(0, 10))}`
          : `Versão ${consent.currentVersion} ainda não aceita`}
      </dd>
    </div>
  )
}

function ExportCard() {
  const exportData = useExportData()

  return (
    <Card>
      <h3 className={sectionTitleClassName}>Baixar meus dados</h3>
      <p className={sectionTextClassName}>
        Receba um arquivo ZIP com tudo o que o FinPro guarda sobre você: cadastro, contas, lançamentos,
        transferências, metas e demais registros em um arquivo legível por máquina (JSON), mais os anexos e a
        foto de perfil. Os grupos compartilhados de que você participa vêm completos.
      </p>

      {exportData.isError && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {exportData.error instanceof ApiError
            ? exportData.error.message
            : 'Não foi possível gerar o arquivo. Tente novamente.'}
        </p>
      )}

      <Button
        type="button"
        variant="brand"
        className="mt-4 w-full sm:w-auto"
        disabled={exportData.isPending}
        onClick={() => exportData.mutate()}
      >
        {exportData.isPending ? 'Gerando arquivo…' : 'Baixar meus dados'}
      </Button>
    </Card>
  )
}

function DeleteAccountCard() {
  const [open, setOpen] = useState(false)

  return (
    <Card className="border-red-200 dark:border-red-900">
      <h3 className="text-base font-semibold text-red-700 dark:text-red-400">Excluir minha conta</h3>
      <p className={sectionTextClassName}>
        Apaga definitivamente seu cadastro, seu espaço pessoal e os grupos em que só você participa. Nos grupos com
        outras pessoas, você sai e os lançamentos continuam com o grupo, sem o seu nome. Antes de confirmar, você
        vê exatamente o que será apagado.
      </p>
      <Button type="button" variant="danger" className="mt-4 w-full sm:w-auto" onClick={() => setOpen(true)}>
        Excluir minha conta
      </Button>
      <DeleteAccountModal open={open} onClose={() => setOpen(false)} />
    </Card>
  )
}
