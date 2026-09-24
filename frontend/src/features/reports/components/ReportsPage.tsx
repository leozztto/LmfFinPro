import { useState } from 'react'
import { FormField, Select } from '@/shared/ui'
import { REPORT_FORMAT_LABELS, REPORT_TYPE_LABELS, type ReportFormat, type ReportType } from '../types'
import { ClientReceiptForm } from './ClientReceiptForm'
import { AccountStatementForm } from './AccountStatementForm'
import { ClientAnnualStatementForm } from './ClientAnnualStatementForm'
import { CategoryExpenseReportForm } from './CategoryExpenseReportForm'
import { IncomeStatementForm } from './IncomeStatementForm'
import { BudgetVsActualReportForm } from './BudgetVsActualReportForm'
import { TransactionExportForm } from './TransactionExportForm'

export function ReportsPage() {
  const [reportType, setReportType] = useState<ReportType>('CLIENT_RECEIPT')
  const [format, setFormat] = useState<ReportFormat>('PDF')

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Relatórios</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Gere documentos e exportações a partir dos seus dados.
        </p>
      </div>

      <div className="grid gap-4 sm:max-w-md sm:grid-cols-2">
        <FormField label="Tipo de relatório" htmlFor="report-type">
          <Select
            id="report-type"
            value={reportType}
            onChange={(e) => setReportType(e.target.value as ReportType)}
          >
            {Object.entries(REPORT_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Formato do arquivo" htmlFor="report-format">
          <Select
            id="report-format"
            value={format}
            onChange={(e) => setFormat(e.target.value as ReportFormat)}
          >
            {Object.entries(REPORT_FORMAT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
      </div>

      {reportType === 'CLIENT_RECEIPT' && <ClientReceiptForm format={format} />}
      {reportType === 'ACCOUNT_STATEMENT' && <AccountStatementForm format={format} />}
      {reportType === 'CLIENT_ANNUAL_STATEMENT' && <ClientAnnualStatementForm format={format} />}
      {reportType === 'CATEGORY_EXPENSE_REPORT' && <CategoryExpenseReportForm format={format} />}
      {reportType === 'INCOME_STATEMENT' && <IncomeStatementForm format={format} />}
      {reportType === 'BUDGET_VS_ACTUAL' && <BudgetVsActualReportForm format={format} />}
      {reportType === 'TRANSACTION_EXPORT' && <TransactionExportForm format={format} />}
    </div>
  )
}
