import { TransferForm } from './TransferForm'
import { TransferList } from './TransferList'

export function TransfersPage() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">Transferências</h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">Mova valores entre suas próprias contas.</p>
      </div>
      <TransferForm />
      <TransferList />
    </div>
  )
}
