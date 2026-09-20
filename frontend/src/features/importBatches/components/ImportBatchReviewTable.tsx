import { Select } from '@/shared/ui'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { useClients } from '@/features/clients/hooks/useClients'
import { useImportBatchTransactions } from '../hooks/useImportBatchTransactions'
import { useReviewImportedTransaction } from '../hooks/useReviewImportedTransaction'

interface ImportBatchReviewTableProps {
  batchId: number
}

export function ImportBatchReviewTable({ batchId }: ImportBatchReviewTableProps) {
  const { data: transactions, isLoading } = useImportBatchTransactions(batchId)
  const { data: categories } = useCategories()
  const { data: clients } = useClients()
  const reviewTransaction = useReviewImportedTransaction()
  const { showToast } = useToast()

  function handleCategoryChange(transactionId: number, clientId: number | null, value: string) {
    reviewTransaction.mutate(
      { batchId, transactionId, input: { categoryId: value ? Number(value) : null, clientId } },
      {
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar a categoria.'),
      },
    )
  }

  function handleClientChange(transactionId: number, categoryId: number | null, value: string) {
    reviewTransaction.mutate(
      { batchId, transactionId, input: { categoryId, clientId: value ? Number(value) : null } },
      {
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível atualizar o cliente.'),
      },
    )
  }

  if (isLoading) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Carregando transações...</p>
  }

  if (!transactions?.length) {
    return <p className="text-sm text-zinc-500 dark:text-zinc-400">Nenhuma transação nesta importação.</p>
  }

  return (
    <div className="space-y-2">
      {transactions.map((transaction) => {
        const matchingCategories = categories?.filter((category) => category.type === transaction.type) ?? []
        const amountClassName = transaction.type === 'INCOME' ? 'text-[#5ab482]' : 'text-[#f06464]'
        const amountLabel = `${transaction.type === 'INCOME' ? '+' : '-'} ${formatCurrency(transaction.amount)}`

        return (
          <div
            key={transaction.id}
            className="grid gap-2 rounded-lg border border-zinc-200 p-3 text-sm dark:border-zinc-700 sm:grid-cols-[1fr_auto_9rem_9rem] sm:items-center"
          >
            <div className="min-w-0">
              <p className="truncate font-medium text-zinc-800 dark:text-zinc-100">{transaction.description}</p>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">{formatDateOnlyBr(transaction.transactionDate)}</p>
            </div>
            <span className={`shrink-0 font-semibold ${amountClassName}`}>{amountLabel}</span>
            <Select
              aria-label="Categoria"
              value={transaction.categoryId ?? ''}
              onChange={(e) => handleCategoryChange(transaction.id, transaction.clientId, e.target.value)}
              className={transaction.categoryId == null ? 'border-amber-400 dark:border-amber-500' : ''}
            >
              <option value="">Sem categoria</option>
              {matchingCategories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
            <Select
              aria-label="Cliente"
              value={transaction.clientId ?? ''}
              onChange={(e) => handleClientChange(transaction.id, transaction.categoryId, e.target.value)}
            >
              <option value="">Sem cliente</option>
              {clients?.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </Select>
          </div>
        )
      })}
    </div>
  )
}
