import { Button, Card } from '@/shared/ui'
import { useAccounts } from '@/features/accounts/hooks/useAccounts'
import { useCategories } from '@/features/categories/hooks/useCategories'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import { useTransactions } from '../hooks/useTransactions'
import { useDeleteTransaction } from '../hooks/useDeleteTransaction'

export function TransactionList() {
  const { data: transactions, isLoading } = useTransactions()
  const { data: accounts } = useAccounts()
  const { data: categories } = useCategories()
  const deleteTransaction = useDeleteTransaction()

  if (isLoading) {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando transações...</p>
  }

  if (!transactions?.length) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">
        Nenhuma transação lançada ainda. Lance a primeira acima.
      </p>
    )
  }

  const accountNameById = new Map(accounts?.map((account) => [account.id, account.name]))
  const categoryNameById = new Map(categories?.map((category) => [category.id, category.name]))

  const sorted = [...transactions].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate))

  return (
    <div className="space-y-3">
      {sorted.map((transaction) => (
        <Card key={transaction.id} className="flex items-center justify-between gap-3">
          <div>
            <p className="font-medium text-slate-900 dark:text-slate-50">{transaction.description}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {formatDateOnlyBr(transaction.transactionDate)} ·{' '}
              {accountNameById.get(transaction.accountId) ?? 'conta desconhecida'}
              {transaction.categoryId ? ` · ${categoryNameById.get(transaction.categoryId) ?? ''}` : ''}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span
              className={
                transaction.type === 'INCOME'
                  ? 'font-semibold text-primary-600'
                  : 'font-semibold text-red-600'
              }
            >
              {transaction.type === 'INCOME' ? '+' : '-'} {formatCurrency(transaction.amount)}
            </span>
            <Button
              variant="secondary"
              onClick={() => deleteTransaction.mutate(transaction.id)}
              disabled={deleteTransaction.isPending}
            >
              Remover
            </Button>
          </div>
        </Card>
      ))}
    </div>
  )
}
