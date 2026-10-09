import { Link } from 'react-router-dom'

/** Id da seção dos Termos de Uso para onde o aviso aponta. */
const FISCAL_NOTICE_ANCHOR = 'aviso-fiscal'

/**
 * Aviso fixo das telas que estimam tributos ou pró-labore. Fica visível, sem botão de dispensar: o
 * texto dos Termos (seção "Aviso fiscal") diz o mesmo.
 */
export function FiscalNotice({ subject }: { subject: string }) {
  return (
    <aside
      role="note"
      className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-950 dark:text-amber-200"
    >
      <strong className="font-semibold">Aviso fiscal:</strong> {subject} são estimativas de referência, calculadas
      a partir do que você lançou. Não substituem seu contador, que deve confirmar os valores antes de qualquer
      recolhimento.{' '}
      <Link to={`/termos#${FISCAL_NOTICE_ANCHOR}`} className="font-medium underline">
        Saiba mais
      </Link>
    </aside>
  )
}
